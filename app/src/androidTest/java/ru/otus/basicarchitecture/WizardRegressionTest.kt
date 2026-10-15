package ru.otus.basicarchitecture

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.Assert.assertTrue
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class WizardRegressionTest {
    @Test
    fun invalidDateBlocksNextAndSelectedInterestSurvivesBackNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.firstName)).perform(replaceText("Иван"))
            onView(withId(R.id.lastName)).perform(replaceText("Петров"))
            onView(withId(R.id.birthday)).perform(replaceText("31022000"), closeSoftKeyboard())
            onView(withId(R.id.birthday)).check(matches(withText("31.02.2000")))
            onView(withId(R.id.nextButton)).check(matches(not(isEnabled())))
            onView(withId(R.id.birthday)).perform(replaceText("01011990"), closeSoftKeyboard())
            onView(withId(R.id.nextButton)).check(matches(isEnabled())).perform(click())
            onView(withId(R.id.addressInput)).perform(replaceText("Москва"), closeSoftKeyboard())
            onView(withId(R.id.nextButton)).perform(click())
            scenario.onActivity {
                val chip = it.findViewById<ChipGroup>(R.id.interestsGroup).getChildAt(0) as Chip
                assertTrue("Interest must be checkable", chip.isCheckable)
            }
            onView(withText("Путешествия")).perform(click()).check(matches(isChecked()))
            onView(withId(R.id.nextButton)).perform(click())
            onView(withId(R.id.summary)).check(matches(withText(containsString("Путешествия"))))
            pressBack()
            onView(withText("Путешествия")).check(matches(isChecked()))
        }
    }
}
