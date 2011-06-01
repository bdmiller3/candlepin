/**
 * Copyright (c) 2009 Red Hat, Inc.
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
package org.fedoraproject.candlepin.pinsetter.tasks;

import org.fedoraproject.candlepin.audit.Statistic;
import org.fedoraproject.candlepin.audit.Statistic.EntryType;
import org.fedoraproject.candlepin.audit.Statistic.ValueType;
import org.fedoraproject.candlepin.audit.StatisticCurator;
import org.fedoraproject.candlepin.config.Config;

import com.google.inject.Inject;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.persistence.EntityManager;

/**
 * StatisticHistoryTask.
 */
public class StatisticHistoryTask implements Job {

    public static final String DEFAULT_SCHEDULE = "0 0 1 * * ?"; // run every
                                                                 // day at 1 AM

    private Config config;
    private EntityManager entityManager;
    private StatisticCurator statCurator;

    private static Logger log = Logger.getLogger(StatisticHistoryTask.class);

    /**
     * Instantiates a new certificate revocation list task.
     *
     * @param conf the conf
     */
    @Inject
    public StatisticHistoryTask(EntityManager entityManager, Config conf,
        StatisticCurator statCurator) {
        this.config = conf;
        this.entityManager = entityManager;
        this.statCurator = statCurator;
    }

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        log.info("Executing Statistic History Job.");

        try {
            String consumerCount = "select count(c) from Consumer c";
            Query consumerQuery = currentSession().createQuery(consumerCount);
            Long count = (Long) consumerQuery.iterate().next();
            Statistic s = new Statistic(EntryType.TotalConsumers,
                ValueType.Raw, null, count.intValue());
            statCurator.create(s);

        }
        catch (HibernateException e) {
            log.error("Cannot store: ", e);
        }
    }

    protected Session currentSession() {
        Session sess = (Session) entityManager.getDelegate();
        return sess;
    }
}
