/*
 * Bolo - A stable and beautiful blogging system based in Solo.
 * Copyright (c) 2020, https://github.com/adlered
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.solo.bolo.waf;

import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.servlet.RequestContext;
import org.b3log.solo.bolo.prop.Options;
import org.b3log.solo.model.Option;
import org.b3log.solo.util.Solos;
import pers.adlered.simplecurrentlimiter.main.SimpleCurrentLimiter;

import javax.servlet.http.HttpServletResponse;

/**
 * <h3>bolo-solo</h3>
 * <p>Web Application Firewall.</p>
 *
 * @author : https://github.com/adlered
 * @date : 2020-05-31
 **/
public class WAF {
    public static boolean POWER = false;

    public static void on() {
        if (!POWER) {
            POWER = true;

            int second = 0;
            int times = 0;

            try {
                second = Integer.parseInt(Options.get(Option.ID_C_WAF_CURRENT_LIMIT_SECOND));
                times = Integer.parseInt(Options.get(Option.ID_C_WAF_CURRENT_LIMIT_TIMES));
            } catch (Exception e) {
                second = 180;
                times = 180;
            }

            WAFstorage.currentLimiter = new SimpleCurrentLimiter(second, times);
            WAFlogger.log("WAF is on. [second=" + second + ", times=" + times + "]");
        }
    }

    public static void off() {
        if (POWER) {
            POWER = false;

            WAFstorage.currentLimiter = null;
            WAFlogger.log("WAF is off.");
        }
    }

    public static void restart() {
        off();
        on();
    }

    public static void set() {
        try {
            String wafPower = Options.get(Option.ID_C_WAF_POWER);
            if (wafPower.equals("on")) {
                WAF.restart();
            } else if (wafPower.equals("off")) {
                WAF.off();
            } else {
                WAF.restart();
            }
        } catch (Exception e) {
            WAF.restart();
        }
    }

    public static boolean in(String requestIP, String requestURL) {
        // 防火墙未开启直接放行
        if (!WAF.POWER) {
            return true;
        }

        if (!(
                requestURL.equals("/articles/random") ||
                        requestURL.equals("/manifest.json") ||
                        requestURL.endsWith("/relevant/articles") ||
                        requestURL.equals("/opensearch.xml") ||
                        requestURL.equals("/admin/logs") ||
                        requestURL.equals("/waf/denied") ||
                        requestIP.equals("0:0:0:0:0:0:0:1") ||
                        requestIP.equals("127.0.0.1") ||
                        requestURL.startsWith("/admin") ||
                        requestURL.startsWith("/console") ||
                        requestURL.startsWith("/plugins") ||
                        requestURL.equals("/oauth/bolo/login")
        )) {

            WAFlogger.logTrace(requestIP, requestURL);
            WAFrule rule = new WAFrule();
            String str = requestIP;

            if (rule.access(str)) {

                return true;
            }

            WAFlogger.log("Request denied of " + requestIP + ".");
            return false;
        }

        return true;
    }

}