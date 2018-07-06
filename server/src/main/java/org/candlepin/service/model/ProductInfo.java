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
import java.util.Map;



/**
 * The ProductInfo represents a minimal set of owner/organization information used by the service
 * adapters.
 *
 * Data which is not set or does not change should be represented by null values. To explicitly
 * clear a value, an empty string or non-null "empty" value should be used instead.
 */
public interface ProductInfo {

    /**
     * Fetches the Red Hat ID of this product. If the ID has not yet been set, this method returns
     * null.
     *
     * @return
     *  the Red Hat ID of this product, or null if the ID has not been set
     */
    String getId();

    /**
     * Fetches the name of this product. If the name has not yet been set, this method returns null.
     *
     * @return
     *  the name of this product, or null if the name has not been set
     */
    String getName();

    /**
     * Fetches the multiplier for this product. If the multiplier has not yet been set, this method
     * returns null.
     *
     * @return
     *  the multiplier for this product, or null if the multiplier has not been set
     */
    Long getMultiplier();

    /**
     * Fetches a collection of IDs of products dependent on this product. If the dependent products
     * have not yet been set, this method returns null. If this product has no dependent products,
     * this method returns an empty collection.
     *
     * @return
     *  a collection of IDs of products dependent on this, or null if the dependent products have
     *  not been set
     */
    Collection<String> getDependentProductIds();

    /**
     * Fetches the attributes of this product. If the attributes have not yet been set, this method
     * returns null. If this product does not have any attributes, this method returns an empty
     * map.
     *
     * @return
     *  the attributes of this product, or null if the attributes have not been set
     */
    Map<String, String> getAttributes();

    /**
     * Fetches a collection of this product's content. If the content has not yet been set, this
     * method returns null. If this product does not have any content, this method returns an empty
     * collection.
     *
     * @return
     *  the content of this product, or null if the content has not been set
     */
    Collection<? extends ProductContentInfo> getProductContent();

}