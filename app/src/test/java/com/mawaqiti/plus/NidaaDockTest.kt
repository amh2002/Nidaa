package com.mawaqiti.plus

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mawaqiti.plus.ui.components.DockDestination
import com.mawaqiti.plus.ui.components.DockMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * beta13: the dock is driven by exactly one registry, [DockDestination], and
 * one geometry object, [DockMetrics].
 *
 * These tests lock what makes "add a future feature = append one entry" safe and
 * what the owner asked for in beta13:
 *
 *  - visual order (declaration order) is Home -> Adhkar -> Qibla -> Settings,
 *    which in Arabic RTL renders rightmost -> leftmost;
 *  - every entry owns a unique tab index (Home = 0, Qibla = 1, Settings = 2,
 *    Adhkar = 3);
 *  - all slots are the same fixed size and all gaps are the same fixed length;
 *  - the indicator is always one slot big and its offset is a pure function of
 *    the selected index, so rapid switching can never strand it;
 *  - an unknown tab resolves to null (content falls back to Home) and index 0.
 */
class NidaaDockTest {

    @Test
    fun `visual order is home then adhkar then qibla then settings`() {
        assertEquals(
            listOf(
                DockDestination.HOME,
                DockDestination.ADHKAR,
                DockDestination.QIBLA,
                DockDestination.SETTINGS
            ),
            DockDestination.ordered
        )
    }

    @Test
    fun `adhkar is the second destination and settings the last`() {
        // beta32: the owner's order - home first, the adhkar hub second, the
        // settings last. Declaration order is the RTL reading order, so this is
        // also what the dock draws right-to-left.
        assertEquals(DockDestination.HOME, DockDestination.ordered.first())
        assertEquals(DockDestination.ADHKAR, DockDestination.ordered[1])
        assertEquals(DockDestination.SETTINGS, DockDestination.ordered.last())
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
        // beta32: the new hub takes a fresh index; the shipped three keep theirs.
        assertEquals(3, DockDestination.ADHKAR.tab)
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
    fun `slots are identical and gaps are equal`() {
        assertEquals(64.dp, DockMetrics.SLOT_WIDTH)
        assertEquals(52.dp, DockMetrics.SLOT_HEIGHT)
        assertEquals(6.dp, DockMetrics.GAP)

        val slots = DockDestination.ordered.size
        // Width = n slots + (n - 1) identical gaps; no SpaceBetween/weights.
        assertEquals(
            DockMetrics.SLOT_WIDTH * slots + DockMetrics.GAP * (slots - 1),
            DockMetrics.dockWidth(slots)
        )
        // The distance between two adjacent slot starts is always slot + gap.
        assertEquals(DockMetrics.SLOT_WIDTH + DockMetrics.GAP, DockMetrics.stride())
    }

    @Test
    fun `indicator is always exactly one slot`() {
        assertEquals(
            DpSize(DockMetrics.SLOT_WIDTH, DockMetrics.SLOT_HEIGHT),
            DockMetrics.indicatorSize()
        )
        assertEquals(64.dp, DockMetrics.indicatorSize().width)
        assertEquals(52.dp, DockMetrics.indicatorSize().height)
    }

    @Test
    fun `indicator offset lands exactly on each slot`() {
        assertEquals(0.dp, DockMetrics.indicatorOffset(0))
        assertEquals(70.dp, DockMetrics.indicatorOffset(1))
        assertEquals(140.dp, DockMetrics.indicatorOffset(2))
        assertEquals(210.dp, DockMetrics.indicatorOffset(3))

        DockDestination.ordered.forEachIndexed { index, destination ->
            assertEquals(
                DockMetrics.stride() * index,
                DockMetrics.indicatorOffset(DockMetrics.indicatorIndex(destination.tab))
            )
        }
    }

    @Test
    fun `rapid switching targets only the final destination`() {
        // Simulates a burst of taps: the target is recomputed from the final
        // tab only, never accumulated from the intermediate ones.
        val sequence = listOf(
            DockDestination.SETTINGS,
            DockDestination.QIBLA,
            DockDestination.HOME,
            DockDestination.SETTINGS,
            DockDestination.QIBLA
        )
        val final = sequence.last()
        val expectedIndex = DockDestination.ordered.indexOf(final)
        assertEquals(
            DockMetrics.indicatorOffset(expectedIndex),
            DockMetrics.indicatorOffset(DockMetrics.indicatorIndex(final.tab))
        )
        // Qibla is the last declaration entry, so the indicator must end at
        // exactly two strides from the start.
        assertEquals(DockMetrics.stride() * 2, DockMetrics.indicatorOffset(
            DockMetrics.indicatorIndex(final.tab)
        ))
    }

    @Test
    fun `unknown tab falls back to the first slot`() {
        assertEquals(0, DockMetrics.indicatorIndex(99))
        assertEquals(0.dp, DockMetrics.indicatorOffset(DockMetrics.indicatorIndex(99)))
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
