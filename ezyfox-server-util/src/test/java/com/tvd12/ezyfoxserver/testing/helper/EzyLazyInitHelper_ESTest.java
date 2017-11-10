/*
 * This file was automatically generated by EvoSuite
 * Wed May 03 09:49:34 ICT 2017
 */

package com.tvd12.ezyfoxserver.testing.helper;

import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.tvd12.ezyfoxserver.function.EzyInitialize;
import com.tvd12.ezyfoxserver.helper.EzyLazyInitHelper;

public class EzyLazyInitHelper_ESTest  {

  @Test
  public void test3()  throws Throwable  {
      Integer integer0 = new Integer(1203);
      // Undeclared exception!
      try { 
        EzyLazyInitHelper.init((Object) integer0, (EzyInitialize<Object>) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
//         assertThrownBy("com.tvd12.ezyfoxserver.helper.EzyLazyInitHelper", e);
      }
  }

  @Test
  public void test4()  throws Throwable  {
      // Undeclared exception!
      try { 
        EzyLazyInitHelper.init((Object) null, (EzyInitialize<Object>) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
//         assertThrownBy("com.tvd12.ezyfoxserver.helper.EzyLazyInitHelper", e);
      }
  }
}
