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
package org.fedoraproject.candlepin.policy.js.entitlement;

import org.fedoraproject.candlepin.config.Config;
import org.fedoraproject.candlepin.model.Consumer;
import org.fedoraproject.candlepin.model.ConsumerCurator;
import org.fedoraproject.candlepin.model.Entitlement;
import org.fedoraproject.candlepin.model.Pool;
import org.fedoraproject.candlepin.policy.Enforcer;
import org.fedoraproject.candlepin.policy.js.JsRules;
import org.fedoraproject.candlepin.policy.js.RuleExecutionException;
import org.fedoraproject.candlepin.policy.js.compliance.ComplianceStatus;
import org.fedoraproject.candlepin.policy.js.pool.PoolHelper;
import org.fedoraproject.candlepin.service.ProductServiceAdapter;
import org.fedoraproject.candlepin.util.DateSource;

import com.google.inject.Inject;

import org.apache.log4j.Logger;
import org.xnap.commons.i18n.I18n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ManifestEntitlementRules - Exists primarily to allow consumers of manifest type
 * to have alternate rules checks.
 */
public class ManifestEntitlementRules extends AbstractEntitlementRules implements Enforcer {

    @Inject
    public ManifestEntitlementRules(DateSource dateSource,
        JsRules jsRules,
        ProductServiceAdapter prodAdapter,
        I18n i18n, Config config, ConsumerCurator consumerCurator) {

        this.jsRules = jsRules;
        this.dateSource = dateSource;
        this.prodAdapter = prodAdapter;
        this.i18n = i18n;
        this.attributesToRules = null;
        this.config = config;
        this.consumerCurator = consumerCurator;

        log = Logger.getLogger(ManifestEntitlementRules.class);
        rulesLogger =
            Logger.getLogger(ManifestEntitlementRules.class.getCanonicalName() + ".rules");
    }

    @Override
    public PoolHelper postEntitlement(
            Consumer consumer, PoolHelper postEntHelper, Entitlement ent) {

        jsRules.reinitTo("entitlement_name_space");
        rulesInit();

        runPostEntitlement(postEntHelper, ent);
        return postEntHelper;
    }

    @Override
    public PreEntHelper preEntitlement(
            Consumer consumer, Pool entitlementPool, Integer quantity) {

        jsRules.reinitTo("entitlement_name_space");
        rulesInit();

        return new PreEntHelper(1, null);
    }

    @Override
    public Map<Pool, Integer> selectBestPools(Consumer consumer, String[] productIds,
        List<Pool> pools, ComplianceStatus compliance)
        throws RuleExecutionException {

        jsRules.reinitTo("entitlement_name_space");
        rulesInit();

        if (pools.isEmpty()) {
            return null;
        }

        Map<Pool, Integer> best = new HashMap<Pool, Integer>();
        for (Pool pool : pools) {
            best.put(pool, 1);
        }
        return best;
    }

    public PreUnbindHelper preUnbind(Consumer consumer, Pool entitlementPool) {
        jsRules.reinitTo("unbind_name_space");
        rulesInit();
        return new PreUnbindHelper(consumerCurator);
    }

    public PoolHelper postUnbind(Consumer c, PoolHelper postHelper, Entitlement ent) {
        jsRules.reinitTo("unbind_name_space");
        rulesInit();
        runPostUnbind(postHelper, ent);
        return postHelper;
    }
}
