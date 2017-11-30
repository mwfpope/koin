package org.koin.test.core

import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import org.koin.Koin
import org.koin.core.scope.Scope
import org.koin.dsl.module.Module
import org.koin.error.BeanInstanceCreationException
import org.koin.error.DependencyResolutionException
import org.koin.log.PrintLogger
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.test.AbstractKoinTest
import org.koin.test.ext.junit.assertContexts
import org.koin.test.ext.junit.assertDefinedInScope
import org.koin.test.ext.junit.assertDefinitions
import org.koin.test.ext.junit.assertScopeParent
import org.koin.test.get

class StackTest : AbstractKoinTest() {

    class FlatContextsModule() : Module() {
        override fun context() = applicationContext {

            provide { ComponentA() }

            context(name = "B") {
                provide { ComponentB(get()) }
            }

            context(name = "C") {
                provide { ComponentC(get()) }
            }
        }
    }

    class HierarchyContextsModule() : Module() {
        override fun context() = applicationContext {
            context(name = "A") {
                provide { ComponentA() }

                context(name = "B") {
                    provide { ComponentB(get()) }

                    context(name = "C") {
                        provide { ComponentC(get()) }
                    }
                }

            }
            provide { ComponentD(get()) }
        }
    }

    class NotVisibleContextsModule() : Module() {
        override fun context() = applicationContext {

            provide { ComponentB(get()) }

            context(name = "A") {
                provide { ComponentA() }
            }

            context(name = "D") {
                provide { ComponentD(get()) }
            }
        }
    }

    class ComponentA
    class ComponentB(val componentA: ComponentA)
    class ComponentC(val componentA: ComponentA)
    class ComponentD(val componentB: ComponentB)

    @Test
    fun `has flat visibility`() {
        Koin.logger = PrintLogger()
        startKoin(listOf(FlatContextsModule()))

        assertContexts(3)
        assertDefinitions(3)

        assertDefinedInScope(ComponentA::class, Scope.ROOT)
        assertDefinedInScope(ComponentB::class, "B")
        assertDefinedInScope(ComponentC::class, "C")

        assertScopeParent("B", Scope.ROOT)
        assertScopeParent("C", Scope.ROOT)

        Assert.assertNotNull(get<ComponentC>())
        Assert.assertNotNull(get<ComponentB>())
        Assert.assertNotNull(get<ComponentA>())
    }

    @Test
    fun `has hierarchic visibility`() {
        Koin.logger = PrintLogger()
        startKoin(listOf(HierarchyContextsModule()))

        Assert.assertNotNull(get<ComponentC>())
        Assert.assertNotNull(get<ComponentB>())
        Assert.assertNotNull(get<ComponentA>())
        try {
            get<ComponentD>()
            fail()
        } catch (e: BeanInstanceCreationException) {

        }
    }

    @Test
    fun `not good visibility context`() {
        Koin.logger = PrintLogger()
        startKoin(listOf(NotVisibleContextsModule()))

        Assert.assertNotNull(get<ComponentA>())
        try {
            get<ComponentB>()
            fail()
        } catch (e: BeanInstanceCreationException) {
        }
        try {
            get<ComponentD>()
            fail()
        } catch (e: DependencyResolutionException) {
        }
    }

}