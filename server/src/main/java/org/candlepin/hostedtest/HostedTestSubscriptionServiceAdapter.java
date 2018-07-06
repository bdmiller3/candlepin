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
package org.candlepin.hostedtest;

import org.candlepin.service.SubscriptionServiceAdapter;
import org.candlepin.service.model.ConsumerInfo;
import org.candlepin.service.model.ProductInfo;
import org.candlepin.service.model.SubscriptionInfo;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * The HostedTestSubscriptionServiceAdapter class is used to provide an
 * in-memory upstream source for subscriptions when candlepin is run in hosted
 * mode, while it is built with candlepin, it is not packaged in candlepin.war,
 * as the only purpose of this class is to support spec tests.
 */
public class HostedTestSubscriptionServiceAdapter implements SubscriptionServiceAdapter {
    private static Logger log = LoggerFactory.getLogger(HostedTestSubscriptionServiceAdapter.class);

    private static Map<String, SubscriptionInfo> idMap = new HashMap<>();
    private static Map<String, List<SubscriptionInfo>> ownerMap = new HashMap<>();
    private static Map<String, List<SubscriptionInfo>> productMap = new HashMap<>();

    @Override
    public Collection<SubscriptionInfo> getSubscriptions(String ownerKey) {
        return this.ownerMap.containsKey(ownerKey) ? this.ownerMap.get(ownerKey) : new ArrayList<>();
    }

    @Override
    public Collection<String> getSubscriptionIds(String ownerKey) {
        List<String> ids = new ArrayList<>();
        List<SubscriptionInfo> subscriptions = ownerMap.get(ownerKey);

        if (subscriptions != null) {
            for (SubscriptionInfo subscription : subscriptions) {
                ids.add(subscription.getId());
            }
        }

        return ids;
    }

    @Override
    public Collection<SubscriptionInfo> getSubscriptionsByProductId(String productId) {
        return this.productMap.containsKey(productId) ? this.productMap.get(productId) : new ArrayList<>();
    }

    @Override
    public SubscriptionInfo getSubscription(String subscriptionId) {
        return idMap.get(subscriptionId);
    }

    @Override
    public Collection<SubscriptionInfo> getSubscriptions() {
        List<SubscriptionInfo> result = new ArrayList<>();
        for (String id : idMap.keySet()) {
            result.add(idMap.get(id));
        }
        return result;
    }

    @Override
    public boolean hasUnacceptedSubscriptionTerms(String ownerKey) {
        return false;
    }

    @Override
    public void sendActivationEmail(String subscriptionId) {
        // method intentionally left blank
    }

    @Override
    public boolean canActivateSubscription(ConsumerInfo consumer) {
        return false;
    }

    @Override
    public void activateSubscription(ConsumerInfo consumer, String email, String emailLocale) {
        // method intentionally left blank
    }

    public SubscriptionInfo createSubscription(SubscriptionInfo s) {
        idMap.put(s.getId(), s);
        if (!ownerMap.containsKey(s.getOwner().getKey())) {
            ownerMap.put(s.getOwner().getKey(), new ArrayList<>());
        }
        ownerMap.get(s.getOwner().getKey()).add(s);
        if (!productMap.containsKey(s.getProduct().getId())) {
            productMap.put(s.getProduct().getId(), new ArrayList<>());
        }

        this.clearUuids(s.getProduct());
        this.clearUuids(s.getDerivedProduct());

        if (CollectionUtils.isNotEmpty(s.getProvidedProducts())) {
            for (ProductInfo pdata : s.getProvidedProducts()) {
                this.clearUuids(pdata);
            }
        }

        if (CollectionUtils.isNotEmpty(s.getDerivedProvidedProducts())) {
            for (ProductInfo pdata : s.getDerivedProvidedProducts()) {
                this.clearUuids(pdata);
            }
        }

        productMap.get(s.getProduct().getId()).add(s);
        return s;
    }

    private void clearUuids(ProductInfo pdata) {
        if (pdata != null) {
            pdata.setUuid(null);
            if (pdata.getProductContent() != null) {
                for (ProductContentData pcdata : pdata.getProductContent()) {
                    if (pcdata.getContent() != null) {
                        pcdata.getContent().setUuid(null);
                    }
                }
            }
        }
    }

    public SubscriptionInfo updateSubscription(SubscriptionInfo ss) {
        deleteSubscription(ss.getId());
        SubscriptionInfo s = createSubscription(ss);
        return s;
    }

    public void deleteSubscription(SubscriptionInfo s) {
        deleteSubscription(s.getId());
    }

    public boolean deleteSubscription(String id) {
        if (idMap.containsKey(id)) {
            SubscriptionInfo s = idMap.remove(id);
            ownerMap.get(s.getOwner().getKey()).remove(s);
            productMap.get(s.getProduct().getId()).remove(s);
            return true;
        }
        return false;
    }

    public void deleteAllSubscriptions() {
        idMap.clear();
        ownerMap.clear();
        productMap.clear();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

}
