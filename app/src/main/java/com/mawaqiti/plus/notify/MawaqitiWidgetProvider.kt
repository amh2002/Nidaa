package com.mawaqiti.plus.notify

/**
 * Main prayer-times widget (4x2 by default, resizable 3x2 … 5x3 since beta 7.9).
 *
 * The behaviour lives in [SafeWidgetProvider]; see [WidgetUpdater] for the
 * rendering rules. Kept as its own class because it is referenced by the
 * manifest, by [WidgetUpdater] and by the existing widget tests.
 */
class MawaqitiWidgetProvider : SafeWidgetProvider()
