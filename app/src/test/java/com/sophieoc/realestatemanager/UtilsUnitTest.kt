package com.sophieoc.realestatemanager

import com.sophieoc.realestatemanager.util.Utils
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.math.roundToInt

class UtilsUnitTest {
    @Test
    fun todayDatePattern_isCorrect() {
        val todayDate = Calendar.getInstance()
        val day = todayDate.get(Calendar.DAY_OF_MONTH)
        val month = todayDate.get(Calendar.MONTH) + 1
        val year = todayDate.get(Calendar.YEAR)
        var dateExpected = "$day/$month/$year"
        if (month <= 9)
            dateExpected = "$day/0$month/$year"
        if (day <= 9)
            dateExpected = "0$dateExpected"
        val todayDateResult = Utils.todayDate
        Assert.assertEquals(dateExpected, todayDateResult)
    }

    @Test
    fun convertDollarToEuro_isCorrect() {
        val dollar = (10..999).random()
        val valueExpected = (dollar * 0.846).roundToInt()
        val result = Utils.convertDollarToEuro(dollar)
        Assert.assertEquals(valueExpected, result)
    }

    @Test
    fun convertEuroToDollar_isCorrect() {
        val euro = (10..999).random()
        val valueExpected = (euro * 1.188).roundToInt()
        val result = Utils.convertEuroToDollar(euro)
        Assert.assertEquals(valueExpected, result)
    }
}