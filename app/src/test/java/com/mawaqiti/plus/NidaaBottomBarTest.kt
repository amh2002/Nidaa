package com.mawaqiti.plus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.mawaqiti.plus.ui.components.NidaaNavItem
import com.mawaqiti.plus.ui.components.NidaaNavItems
import com.mawaqiti.plus.ui.components.navTabIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta11: the bottom bar is list-driven, so these tests lock the contract that
 * makes "add a destination = add one entry" safe:
 *
 *  - the two shipped routes and their order (order is RTL order: first = right);
 *  - every route resolves to `index + 1`, and the two existing screens keep
 *    their historical tab indices (Qibla = 1, Settings = 2);
 *  - appending an entry only allocates a new index and never renumbers the
 *    existing ones;
 *  - an unknown route falls back to Home (0) instead of crashing.
 */
class NidaaBottomBarTest {

    @Test
    fun `shipped bar is exactly qibla then settings`() {
        assertEquals(listOf("qibla", "settings"), NidaaNavItems.map { it.route })
    }

    @Test
    fun `routes are unique and every item has a label and an icon`() {
        assertEquals(NidaaNavItems.size, NidaaNavItems.map { it.route }.toSet().size)
        NidaaNavItems.forEach { item ->
            assertTrue(item.route.isNotBlank())
            assertTrue(item.labelRes != 0)
            assertTrue(item.icon.name.isNotBlank())
        }
    }

    @Test
    fun `existing destinations keep their tab indices`() {
        assertEquals(1, navTabIndex(NidaaNavItems, "qibla"))
        assertEquals(2, navTabIndex(NidaaNavItems, "settings"))
    }

    @Test
    fun `appending a destination allocates the next index only`() {
        val extended = NidaaNavItems + NidaaNavItem(
            route = "future",
            labelRes = R.string.footer_settings,
            icon = Icons.Filled.Check
        )
        assertEquals(1, navTabIndex(extended, "qibla"))
        assertEquals(2, navTabIndex(extended, "settings"))
        assertEquals(3, navTabIndex(extended, "future"))
        assertEquals(0, navTabIndex(extended, "missing"))
    }

    @Test
    fun `unknown route falls back to home`() {
        assertEquals(0, navTabIndex(NidaaNavItems, "about"))
        assertEquals(0, navTabIndex(emptyList(), "qibla"))
    }
}
