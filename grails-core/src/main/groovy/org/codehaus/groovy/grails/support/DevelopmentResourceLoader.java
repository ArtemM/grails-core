/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.support;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Allows files references like /WEB-INF/grails-app to be loaded from ./grails-app to support
 * the difference between wAR deployment and run-app.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DevelopmentResourceLoader extends DefaultResourceLoader{

    private String baseLocation = ".";
    private GrailsApplication application;
    private static final String PLUGINS_PREFIX = "plugins/";
    private static final String SLASH = "/";

    public DevelopmentResourceLoader(GrailsApplication application) {
        this.application = application;
    }

    public DevelopmentResourceLoader() {
        super();
    }

    public DevelopmentResourceLoader(GrailsApplication application, String baseLocation) {
        this(application);
        this.baseLocation = baseLocation;
    }

    @Override
    public Resource getResource(String location) {
        if (Metadata.getCurrent().isWarDeployed()) {
            return super.getResource(location);
        }

        location = getRealLocationInProject(location);
        return super.getResource(location);
    }

    /**
     * Retrieves the real location of a GSP within a Grails project.
     * @param location The location of the GSP at deployment time
     * @return The location of the GSP at development time
     */
    protected String getRealLocationInProject(String location) {

        if (!location.startsWith(SLASH)) location = SLASH + location;

        if (!location.startsWith(GrailsResourceUtils.WEB_INF)) {
            return GrailsResourceUtils.WEB_APP_DIR+location;
        }

        final String noWebInf = location.substring(GrailsResourceUtils.WEB_INF.length() + 1);
        final String defaultPath = "file:" + baseLocation + SLASH + noWebInf;
        if (!noWebInf.startsWith(PLUGINS_PREFIX)) {
            return defaultPath;
        }

        if (application != null) {

            BuildSettings settings = BuildSettingsHolder.getSettings();
            PluginBuildSettings pluginBuildSettings = org.codehaus.groovy.grails.plugins.GrailsPluginUtils.getPluginBuildSettings();
            String pluginPath = StringUtils.substringAfter(noWebInf, SLASH);
            String pluginName = StringUtils.substringBefore(pluginPath, SLASH);
            String remainingPath = StringUtils.substringAfter(pluginPath, SLASH);
            Resource r = pluginBuildSettings.getPluginDirForName(pluginName);
            if (r != null) {
                try {
                    return "file:" + r.getFile().getAbsolutePath() + SLASH + remainingPath;
                }
                catch (IOException e) {
                    return defaultPath;
                }
            }

            if (settings != null) {
                return "file:" + settings.getProjectPluginsDir().getAbsolutePath() + SLASH + pluginName + SLASH + remainingPath;
            }
        }

        return defaultPath;
    }

    private GrailsPluginManager getPluginManager(ApplicationContext mainContext) {
        if (mainContext != null) {
            return mainContext.getBean(GrailsPluginManager.class);
        }
        return null;
    }
}
