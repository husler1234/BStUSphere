package com.example

import com.example.ui.screens.encodeQrMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun encodeQrMatrix_generatesValidSquareMatrixForDownloadUrl() {
    val matrix = encodeQrMatrix("https://example.github.io/BStUsphere/")
    assertNotNull(matrix)
    assertTrue(matrix!!.width >= 21)
    assertEquals(matrix.width, matrix.height)
  }
}
