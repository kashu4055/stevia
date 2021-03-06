package com.persado.oss.quality.stevia.selenium.listeners;

/*
 * #%L
 * Stevia QA Framework - Core
 * %%
 * Copyright (C) 2013 - 2014 Persado
 * %%
 * Copyright (c) Persado Intellectual Property Limited. All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  
 * * Neither the name of the Persado Intellectual Property Limited nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.persado.oss.quality.stevia.annotations.RunsWithController;
import com.persado.oss.quality.stevia.annotations.AnnotationsHelper;
import com.persado.oss.quality.stevia.selenium.core.SteviaContext;

public class ControllerMaskingListener implements IInvokedMethodListener2 {

	public static final Logger LOG = LoggerFactory.getLogger(ControllerMaskingListener.class);

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
	}

	//boolean is necessary for cases that the context is clean (spring context does not exist) and we've not masked anyway
	boolean masked = false;
	
	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		Method rmethod = method.getTestMethod().getConstructorOrMethod().getMethod();
		if (rmethod.getAnnotation(Test.class) != null || 
			rmethod.getAnnotation(BeforeClass.class) != null || 
			rmethod.getAnnotation(BeforeTest.class) != null) {
			if (rmethod.getAnnotation(RunsWithController.class) != null || 
				rmethod.getDeclaringClass().getAnnotation(RunsWithController.class) != null) {
				LOG.warn("Method or Class of {} asks Controller to be masked", rmethod.getName());
				AnnotationsHelper p = SteviaContext.getSpringContext().getBean(AnnotationsHelper.class);
				try {
					p.maskExistingController(rmethod);
					masked = true;
				} catch (Throwable e) {
					throw new IllegalStateException("failed to replace controller",e);
				}
			}
		}
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
		if (masked) {
			AnnotationsHelper p = SteviaContext.getSpringContext().getBean(AnnotationsHelper.class);
			try {
				p.controllerUnmask();
				masked = false;
			} catch (Throwable e) {
				throw new IllegalStateException("failed to replace masked controller",e);
			}
		}
	}

}
