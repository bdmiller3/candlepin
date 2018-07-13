/**
 * Copyright (c) 2009 - 2018 Red Hat, Inc.
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
package org.candlepin.service.model;

import java.util.Collection;



/**
 * The SubscriptionInfo represents a minimal set of subscription information used by the
 * service adapters.
 *
 * Data which is not set or does not change should be represented by null values. To explicitly
 * clear a value, an empty string or non-null "empty" value should be used instead.
 */
public interface SubscriptionInfo {

    /**
     * Fetches the ID of this subscription. If the ID has not yet been set, this method returns
     * null.
     *
     * @return
     *  The ID of this subscription, or null if the ID has not been set
     */
    String getId();

    /**
     * Fetches the owner of this subscription. If the owner has not yet been set, this method
     * returns null.
     *
     * @return
     *  The owner of this subscription, or null if the owner has not been set
     */
    OwnerInfo getOwner();

    /**
     * Fetches the marketing product (SKU) for this subscription. If the marketing product has not
     * yet been set, this method returns null.
     *
     * @return
     *  The marketing product for this subscription, or null if the product has not been set
     */
    ProductInfo getProduct();

    /**
     * Fetches a collection of engineering products this subscription provides. If the provided
     * products have not yet been set, this method returns null. If this subscription does not
     * provide any engineering products, this method returns an empty collection.
     *
     * @return
     *  A collection of engineering products provided by this subscription, or null if the provided
     *  products have not been set
     */
    Collection<? extends ProductInfo> getProvidedProducts();

    /**
     * Fetches the derived marketing product (SKU) provided to guests of hosts consuming this
     * subscription. If the derived product has not been set, this method returns null.
     *
     * @return
     *  The derived marketing product for this subscription, or null if the derived product has not
     *  been set
     */
    ProductInfo getDerivedProduct();

    /**
     * Fetches a collection of derived engineering products provided to guests of hosts consuming
     * this subscription. If the derived provided products have not been set, this method returns
     * null. If this subscription does not provide any derived products, this method returns an
     * empty collection.
     *
     * @return
     *  A collection of engineering products provided by this subscription, or null if the derived
     *  provided products have not been set
     */
    Collection<? extends ProductInfo> getDerivedProvidedProducts();

}