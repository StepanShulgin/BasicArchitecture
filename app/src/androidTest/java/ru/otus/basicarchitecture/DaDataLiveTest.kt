package ru.otus.basicarchitecture

import android.os.SystemClock
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class DaDataLiveTest {
    @Test
    fun realSuggestionCanBeSelectedAndSavedInSummary() {
        // Opt-in integration check: ordinary builds without a private key still run offline tests.
        assumeTrue(BuildConfig.DADATA_API_KEY.isNotBlank())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.firstName)).perform(replaceText("Иван"))
            onView(withId(R.id.lastName)).perform(replaceText("Петров"))
            onView(withId(R.id.birthday)).perform(replaceText("01011990"), closeSoftKeyboard())
            onView(withId(R.id.nextButton)).perform(click())
            // Close the keyboard before starting the request: a fast response must not
            // open the popup while the keyboard is still changing its position.
            onView(withId(R.id.addressInput)).perform(click(), closeSoftKeyboard())
            onView(withId(R.id.addressInput)).perform(
                replaceText("Москва Хабаровская")
            )
            val deadline = SystemClock.uptimeMillis() + 20_000
            var state: AddressSuggestionState = AddressSuggestionState.Idle
            do {
                scenario.onActivity { activity ->
                    val fragment = requireNotNull(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainer))
                    state = ViewModelProvider(fragment)[AddressViewModel::class.java].suggestions.value
                }
                if (state is AddressSuggestionState.Success || state is AddressSuggestionState.Error) break
                Thread.sleep(100)
            } while (SystemClock.uptimeMillis() < deadline)
            val suggestions = (state as? AddressSuggestionState.Success)?.suggestions
                ?: error("DaData did not return suggestions: $state")
            assertFalse(suggestions.isEmpty())
            val selected = suggestions.first()
            onData(equalTo(selected.value)).inRoot(isPlatformPopup()).perform(waitForStablePopup(), click())
            onView(withId(R.id.addressInput)).check(matches(withText(selected.value)))
            onView(withId(R.id.nextButton)).perform(closeSoftKeyboard(), click())
            onView(withId(R.id.nextButton)).perform(click())
            onView(withId(R.id.summary)).check(matches(withText(containsString(selected.unrestrictedValue))))
        }
    }

    private fun waitForStablePopup(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription() = "wait for the dropdown layout to settle before tapping"

        override fun perform(uiController: UiController, view: View) {
            val deadline = SystemClock.uptimeMillis() + 5_000
            var stableSince = SystemClock.uptimeMillis()
            var previousBounds = Rect()
            do {
                val bounds = Rect()
                val visible = view.getGlobalVisibleRect(bounds)
                val now = SystemClock.uptimeMillis()
                if (!visible || view.isLayoutRequested || bounds != previousBounds) {
                    stableSince = now
                    previousBounds = bounds
                } else if (now - stableSince >= 500) {
                    return
                }
                uiController.loopMainThreadForAtLeast(16)
            } while (SystemClock.uptimeMillis() < deadline)
            throw AssertionError("Dropdown did not reach a stable layout before the timeout")
        }
    }
}
