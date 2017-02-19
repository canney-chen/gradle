/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.caching.configuration

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class BuildCacheConfigurationIntegrationTest extends AbstractIntegrationSpec {
    def setup() {
        buildFile << """
            task assertLocalCacheConfigured {
                doLast {
                    assert gradle.services.get(BuildCacheConfiguration).local.directory == "expected"
                }
            }
        """
    }

    def "can configure with settings.gradle"() {
        settingsFile << """
            buildCache {
                local {
                    directory = "expected"
                }
            }
        """
        expect:
        succeeds("assertLocalCacheConfigured")
    }

    def "can configure with init script"() {
        def initScript = file("initBuildCache.gradle") << """
            gradle.settingsEvaluated { settings ->
                settings.buildCache {
                    local {
                        directory = "expected"
                    }
                }
            }
        """
        expect:
        executer.usingInitScript(initScript)
        succeeds("assertLocalCacheConfigured")
    }

    def "configuration in init script wins over settings.gradle"() {
        def initScript = file("initBuildCache.gradle") << """
            gradle.settingsEvaluated { settings ->
                settings.buildCache {
                    local {
                        directory = "expected"
                    }
                }
            }
        """
        settingsFile << """
            buildCache {
                local {
                    directory = "wrong"
                }
            }
        """
        expect:
        executer.usingInitScript(initScript)
        succeeds("assertLocalCacheConfigured")
    }

    def "buildSrc and project builds configured separately"() {
        def configuration = { path ->
            """
            buildCache {
                local {
                    directory = "$path"
                }
            }
            """
        }
        settingsFile << configuration("expected")
        file("buildSrc/settings.gradle") << configuration("buildSrc-expected")
        file("buildSrc/build.gradle") << """
            apply plugin: 'groovy'

            task assertLocalCacheConfigured {
                doLast {
                    assert gradle.services.get(BuildCacheConfiguration).local.directory == "buildSrc-expected"
                }
            }
            
            build.dependsOn assertLocalCacheConfigured
        """
        expect:
        succeeds("assertLocalCacheConfigured")
    }
}
