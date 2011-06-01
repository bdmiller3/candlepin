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
package org.fedoraproject.candlepin.audit;

import org.fedoraproject.candlepin.model.AbstractHibernateCurator;

import com.wideplay.warp.persist.Transactional;

/**
 * ContentCurator
 */
public class StatisticCurator extends AbstractHibernateCurator<Statistic> {

    public StatisticCurator() {
        super(Statistic.class);
    }

    @Transactional
    public Statistic create(Statistic s) {
        super.create(s);
        return s;
    }
}
