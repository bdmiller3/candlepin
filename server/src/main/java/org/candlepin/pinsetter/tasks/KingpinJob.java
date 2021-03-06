/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.pinsetter.tasks;

import static org.quartz.impl.matchers.NameMatcher.jobNameEquals;

import org.candlepin.audit.EventSink;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.guice.CandlepinRequestScope;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.PinsetterJobListener;
import org.candlepin.pinsetter.core.RetryJobException;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.util.Traceable;

import com.google.inject.Inject;
import com.google.inject.persist.UnitOfWork;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;



/**
 * KingpinJob replaces TransactionalPinsetterJob, which encapsulated
 * other jobs.  Using a supertype between actual jobs and the
 * Quartz Job type gives us more freedom to define behavior.
 * Every candlepin job must extend KingpinJob
 */
public abstract class KingpinJob implements Job {

    private static Logger log = LoggerFactory.getLogger(KingpinJob.class);
    @Inject protected UnitOfWork unitOfWork;
    @Inject protected Configuration config;
    @Inject private EventSink eventSink;
    @Inject private CandlepinRequestScope candlepinRequestScope;

    protected static String prefix = "job";

    @Override
    @Traceable(startable = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {

        long startTime = System.currentTimeMillis();
        candlepinRequestScope.enter();

        // Store the job's unique ID in log4j's thread local MDC, which will automatically
        // add it to all log entries executed for this job.
        try {
            MDC.put("requestType", "job");
            MDC.put("requestUuid", context.getJobDetail().getKey().getName());
        }
        catch (NullPointerException npe) {
            //this can occur in testing
        }

        // Check if this job is running within the context of a specific org
        if (context != null) {
            JobDataMap map = context.getMergedJobDataMap();

            String orgKey = null;
            String orgLogLevel = null;

            if (map != null) {
                // Impl note: we use the OWNER_ID map key to store the org key
                orgKey = map.getString(JobStatus.OWNER_ID);
                orgLogLevel = map.getString(JobStatus.OWNER_LOG_LEVEL);
            }

            if (orgKey != null) {
                MDC.put("org", orgKey);
            }

            if (orgLogLevel != null) {
                MDC.put("orgLogLevel", orgLogLevel);
            }
        }

        // Log the job start time if the job is configured to do so
        if (logExecutionTime()) {
            log.info("Starting job: {}", getClass().getName());
        }

        /*
         * Execute our 'real' job inside a custom unit of work scope, instead
         * of the guice provided one, which is HTTP request scoped.
         */
        boolean startedUow = startUnitOfWork();
        try {
            toExecute(context);
            if (eventSink != null) {
                eventSink.sendEvents();
            }
        }
        /*
         * Very important exception handling here, in some cases we want to allow the
         * possibility of re-trying jobs which fail for a variety of reasons. (typically
         * refresh pools job) Possible re-try scenarios:
         *
         *  - one job attempts to delete a pool that was deleted by another job
         *  - one job attempts to add a pool that was already added
         *  - one job attempts to update a pool that was already updated
         *  - concurrent inserts deadlocking due to mysql gap locking
         *
         *  These can surface as a PersistenceException, also as other runtime exceptions
         *  from specific JDBC drivers which wrap SQLException. We let the jobs themselves
         *  sort this out and throw a specific exception to indicate a retry is an option.
         */
        catch (PersistenceException e) {
            refireCheck(context, e);
            if (eventSink != null) {
                eventSink.rollback();
            }
        }
        catch (RetryJobException e) {
            refireCheck(context, e);
            if (eventSink != null) {
                eventSink.rollback();
            }
        }
        finally {
            candlepinRequestScope.exit();
            if (startedUow) {
                endUnitOfWork();
            }
            if (logExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("Job completed: time={}", executionTime);
            }
        }
    }

    private void refireCheck(JobExecutionContext context, Exception e)
        throws JobExecutionException {

        int maxRefires = getMaxRetries();
        // If the maximum is sub-zero, do not enforce any limit
        boolean refire = maxRefires < 0 || context.getRefireCount() < maxRefires;
        log.error("Persistence exception caught running pinsetter task. Attempt: {}, Refire: {}",
            context.getRefireCount(), refire, e);

        throw new JobExecutionException(e, refire);
    }

    /**
     * Method for actual execution, execute handles unitOfWork for us
     * @param context
     * @throws JobExecutionException if there's a problem executing the job
     */
    public abstract void toExecute(JobExecutionContext context)
        throws JobExecutionException;

    public static JobStatus scheduleJob(JobCurator jobCurator, Scheduler scheduler, JobDetail detail,
        Trigger trigger) throws SchedulerException {

        scheduler.getListenerManager().addJobListenerMatcher(
            PinsetterJobListener.LISTENER_NAME,
            jobNameEquals(detail.getKey().getName()));

        JobStatus status = null;
        try {
            status = jobCurator.create(new JobStatus(detail, trigger == null));
            if (trigger != null) {
                scheduler.scheduleJob(detail, trigger);
            }
            else {
                scheduler.addJob(detail, false);
            }
        }
        catch (EntityExistsException e) {
            // status exists, let's update it
            // in theory this should be the rare case
            status = jobCurator.get(detail.getKey().getName());
            jobCurator.merge(status);
        }
        catch (RuntimeException e) {
            failStatus(jobCurator, status);
            throw e;
        }
        catch (SchedulerException e) {
            failStatus(jobCurator, status);
            throw e;
        }

        return status;
    }

    private static void failStatus(JobCurator curator, JobStatus status) {
        // if there was any error in scheduling, ensure that the status is updated
        if (status != null) {
            status.setState(JobStatus.JobState.FAILED);
            curator.merge(status);
        }
    }

    public static boolean isSchedulable(JobCurator jobCurator, JobStatus status) {
        return true;
    }

    protected boolean startUnitOfWork() {
        if (unitOfWork != null) {
            try {
                unitOfWork.begin();
                return true;
            }
            catch (IllegalStateException e) {
                log.debug("Already have an open unit of work");
                return false;
            }
        }
        return false;
    }

    protected void endUnitOfWork() {
        if (unitOfWork != null) {
            try {
                unitOfWork.end();
            }
            catch (IllegalStateException e) {
                log.debug("Unit of work is already closed, doing nothing");
                // If there is no active unit of work, there is no reason to close it
            }
        }
    }

    private int getMaxRetries() {
        int maxRetries = ConfigProperties.PINSETTER_MAX_RETRIES_DEFAULT;
        try {
            // config may be null if this Job object has been created by hand
            if (config != null) {
                maxRetries = config.getInt(ConfigProperties.PINSETTER_MAX_RETRIES);
            }
        }
        catch (Exception ce) {
            log.warn("Unable to read '" + ConfigProperties.PINSETTER_MAX_RETRIES +
                "' from candlepin config.  Using default of " + maxRetries);
        }
        return maxRetries;
    }

    // Override in jobs to disable execution time logging.
    protected boolean logExecutionTime() {
        return true;
    }
}
