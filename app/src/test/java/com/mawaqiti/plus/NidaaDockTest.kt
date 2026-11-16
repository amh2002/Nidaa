package com.mawaqiti.plus

import com.mawaqiti.plus.ui.components.DockDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta12: the dock is driven by exactly one registry, [DockDestination].
 *
 * These tests lock the parts that make "add a future feature = append one entry"
 * safe:
 *
 *  - visual order (declaration order) is Settings -> Home -> Qibla, which in
 *    Arabic RTL renders rightmost -> leftmost as requested;
 *  - every entry owns a unique tab index and the three shipped values keep the
 *    beta11 tab numbering (Home = 0, Qibla = 1, Settings = 2);
 *  - [DockDestination.forTab] is the only mapping content needs, and an unknown
 *    tab resolves to null (callers fall back to Home) instead of crashing;
 *  - every entry has a label resource and an icon, and selected state is driven
 *    purely by comparing the entry's tab with the current one.
 */
class NidaaDockTest {

    @Test
    fun `visual order is settings then home then qibla`() {
        assertEquals(
            listOf(
                DockDestination.SETTINGS,
                DockDestination.HOME,
                DockDestination.QIBLA
            ),
            DockDestination.ordered
        )
    }

    @Test
    fun `ordered is declaration order and stable`() {
        assertEquals(DockDestination.entries.toList(), DockDestination.ordered)
    }

    @Test
    fun `shipped destinations keep their tab indices`() {
        assertEquals(0, DockDestination.HOME.tab)
        assertEquals(1, DockDestination.QIBLA.tab)
        assertEquals(2, DockDestination.SETTINGS.tab)
    }

    @Test
    fun `tab indices are unique`() {
        val tabs = DockDestination.entries.map { it.tab }
        assertEquals(tabs.size, tabs.toSet().size)
    }

    @Test
    fun `forTab maps every destination and rejects unknown tabs`() {
        DockDestination.entries.forEach { destination ->
            assertEquals(destination, DockDestination.forTab(destination.tab))
        }
        assertNull(DockDestination.forTab(99))
        assertNull(DockDestination.forTab(-1))
    }

    @Test
    fun `every destination has a label and an icon`() {
        DockDestination.entries.forEach { destination ->
            assertTrue(destination.labelRes != 0)
            assertTrue(destination.icon.name.isNotBlank())
        }
    }

    @Test
    fun `selected state follows the tab comparison`() {
        DockDestination.entries.forEach { destination ->
            assertEquals(
                destination == DockDestination.QIBLA,
                destination.tab == DockDestination.QIBLA.tab
            )
        }
    }
}
