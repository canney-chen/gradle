/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Namer
import org.gradle.api.reflect.ObjectInstantiationException
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.TestUtil
import spock.lang.Specification

class FactoryNamedDomainObjectContainerSpec extends Specification {
    final NamedDomainObjectFactory<String> factory = Mock()
    final Instantiator instantiator = TestUtil.instantiatorFactory().decorateLenient()
    def collectionFactory = TestUtil.domainObjectCollectionFactory()
    final namer = { it } as Namer

    def usesFactoryToCreateContainerElements() {
        def container = collectionFactory.newContainer(FactoryNamedDomainObjectContainer, String, String, namer, factory, MutationGuards.identity())

        when:
        def result = container.create('a')

        then:
        result == 'element a'
        1 * factory.create('a') >> 'element a'
        0 * _._
    }

    def usesClosureToCreateContainerElements() {
        def cl = { name -> "element $name" as String }
        def container = collectionFactory.newContainer(FactoryNamedDomainObjectContainer, String, String, namer, cl, MutationGuards.identity())

        when:
        def result = container.create('a')

        then:
        result == 'element a'
        0 * _._
    }

    // Tests for reflective instantiation

    def type
    def extraArgs = []
    def name = "test"

    protected getInstance() {
        getInstance(name)
    }

    protected getInstance(String name) {
        collectionFactory.newContainer(FactoryNamedDomainObjectContainer, type, type, new ReflectiveNamedDomainObjectFactory(type, instantiator, *extraArgs)).create(name)
    }

    static class JustName implements Named {
        String name

        JustName(String name) {
            this.name = name
        }
    }

    def "can create instance with just name constructor"() {
        given:
        type = JustName

        expect:
        instance.name == name
    }

    def "specifying extra args that the type can't handle produces exception"() {
        given:
        type = JustName
        extraArgs = [1, 2]

        when:
        getInstance()

        then:
        def e = thrown(ObjectInstantiationException)
        e.message == "Could not create an instance of type ${JustName.name}."
        e.cause instanceof IllegalArgumentException
        e.cause.message == "Too many parameters provided for constructor for type FactoryNamedDomainObjectContainerSpec.JustName. Expected 1, received 3."
    }

    static class NoConstructor implements Named {
        @Override
        String getName() {
            return "abc"
        }
    }

    def "type with no name constructor produces exception"() {
        given:
        type = NoConstructor

        when:
        getInstance()

        then:
        def e = thrown(ObjectInstantiationException)
        e.cause instanceof IllegalArgumentException
        e.cause.message == 'Too many parameters provided for constructor for type FactoryNamedDomainObjectContainerSpec.NoConstructor. Expected 0, received 1.'
    }

    static class ExtraArgs implements Named {
        String name
        int arg1
        int arg2

        ExtraArgs(name, arg1, arg2) {
            this.name = name
            this.arg1 = arg1
            this.arg2 = arg2
        }
    }

    def "can supply extra args"() {
        given:
        type = ExtraArgs
        extraArgs = [1, 2]

        when:
        def instance = getInstance()

        then:
        instance.name == name
        instance.arg1 == 1
        instance.arg2 == 2
    }

}
