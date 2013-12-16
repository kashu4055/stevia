package com.persado.oss.quality.stevia.selenium.core.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.HasTouchScreen;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.interactions.TouchScreen;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.opera.core.systems.OperaDriver;
import com.persado.oss.quality.stevia.selenium.core.Constants;
import com.persado.oss.quality.stevia.selenium.core.SteviaContext;
import com.persado.oss.quality.stevia.selenium.core.WebController;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.*;

public final class SteviaWebControllerFactory implements Constants {

	public static final Logger LOG = LoggerFactory.getLogger(SteviaWebControllerFactory.class);

	public static WebController getWebController(ApplicationContext context) throws MalformedURLException {
		WebController controller = null;
		if (SteviaContext.getParam(DRIVER_TYPE).contentEquals("webdriver")) {
			controller = new WebDriverWebControllerFactoryImpl().initialize(context, (WebController) context.getBean("webDriverController"));
		} else if (SteviaContext.getParam(DRIVER_TYPE).contentEquals("selenium")) {
			controller = new SeleniumWebControllerFactoryImpl().initialize(context, (WebController) context.getBean("seleniumController"));
		} else if(SteviaContext.getParam(DRIVER_TYPE).contentEquals("appium-ios")) {
            controller = new AppiumIOSControllerFactoryImpl().initialize(context, (WebController) context.getBean("appiumIOSController"));
        } else if(SteviaContext.getParam(DRIVER_TYPE).contentEquals("appium-android")) {
            controller = new AppiumAndroidControllerFactoryImpl().initialize(context, (WebController) context.getBean("appiumAndroidController"));
        }
		return controller;
	}

	public static WebController getWebController(ApplicationContext context, Class<? extends WebController> requestedControllerClass) {
		WebController controller = context.getBean(requestedControllerClass);
		if (controller instanceof WebDriverWebController) {
			controller = new WebDriverWebControllerFactoryImpl().initialize(context, controller);
		} else if (controller instanceof SeleniumWebController){
			controller = new SeleniumWebControllerFactoryImpl().initialize(context, controller);
		} else if (controller instanceof AppiumIOSWebController){
            controller = new AppiumIOSControllerFactoryImpl().initialize(context,controller);
        }
		return controller;
	}

	static class WebDriverWebControllerFactoryImpl implements WebControllerFactory {
		@Override
		public WebController initialize(ApplicationContext context, WebController controller) {
			WebDriverWebController wdController = (WebDriverWebController) controller;
			WebDriver driver = null;
			if (SteviaContext.getParam(DEBUGGING).compareTo(TRUE) == 0) { // debug=on
				if (SteviaContext.getParam(BROWSER) == null || SteviaContext.getParam(BROWSER).compareTo("firefox") == 0
						|| SteviaContext.getParam(BROWSER).isEmpty()) {
					if (SteviaContext.getParam(PROFILE) == null || SteviaContext.getParam(PROFILE).isEmpty()) {
						LOG.info("Debug enabled, using Firefox Driver");
						driver = new FirefoxDriver();
					} else {
						LOG.info("Debug enabled, using a local Firefox profile with FirefoxDriver");
						ProfilesIni allProfiles = new ProfilesIni();
						FirefoxProfile ffProfile = allProfiles.getProfile(PROFILE);
						driver = new FirefoxDriver(ffProfile);
					}
				} else if (SteviaContext.getParam(BROWSER).compareTo("chrome") == 0) {
					LOG.info("Debug enabled, using ChromeDriver");
					driver = new ChromeDriver();
				} else if (SteviaContext.getParam(BROWSER).compareTo("iexplorer") == 0) {
					LOG.info("Debug enabled, using InternetExplorerDriver");
					driver = new InternetExplorerDriver();
				} else if (SteviaContext.getParam(BROWSER).compareTo("safari") == 0) {
					LOG.info("Debug enabled, using SafariDriver");
					driver = new SafariDriver();
				} else if (SteviaContext.getParam(BROWSER).compareTo("opera") == 0) {
					LOG.info("Debug enabled, using OperaDriver");
					driver = new OperaDriver();
				} else {
					throw new IllegalArgumentException(WRONG_BROWSER_PARAMETER);
				}

			} else { // debug=off
				DesiredCapabilities capability = new DesiredCapabilities();
				if (SteviaContext.getParam(BROWSER) == null || SteviaContext.getParam(BROWSER).compareTo("firefox") == 0
						|| SteviaContext.getParam(BROWSER).isEmpty()) {
					LOG.info("Debug OFF, using a RemoteWebDriver with Firefox capabilities");
					capability = DesiredCapabilities.firefox();
				} else if (SteviaContext.getParam(BROWSER).compareTo("chrome") == 0) {
					LOG.info("Debug OFF, using a RemoteWebDriver with Chrome capabilities");
					capability = DesiredCapabilities.chrome();
				} else if (SteviaContext.getParam(BROWSER).compareTo("iexplorer") == 0) {
					LOG.info("Debug OFF, using a RemoteWebDriver with Internet Explorer capabilities");
					capability = DesiredCapabilities.internetExplorer();
				} else if (SteviaContext.getParam(BROWSER).compareTo("safari") == 0) {
					LOG.info("Debug OFF, using a RemoteWebDriver with Safari capabilities");
					capability = DesiredCapabilities.safari();
				} else if (SteviaContext.getParam(BROWSER).compareTo("opera") == 0) {
					LOG.info("Debug OFF, using a RemoteWebDriver with Opera capabilities");
					capability = DesiredCapabilities.opera();
				} else {
					throw new IllegalArgumentException(WRONG_BROWSER_PARAMETER);
				}
				Augmenter augmenter = new Augmenter(); // adds screenshot
														// capability
														// to a default web
														// driver.

				try {
					driver = augmenter.augment(new RemoteWebDriver(new URL("http://" + SteviaContext.getParam(RC_HOST) + ":" + SteviaContext.getParam(RC_PORT)
							+ "/wd/hub"), capability));
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}

			}

			if (SteviaContext.getParam(TARGET_HOST_URL) != null) {
				driver.get(SteviaContext.getParam(TARGET_HOST_URL));
			}
			// driver.manage().window().maximize();
			wdController.setDriver(driver);
			if (SteviaContext.getParam(ACTIONS_LOGGING).compareTo(TRUE) == 0) {
				wdController.enableActionsLogging();
			}
			return wdController;
		}

	}

	static class SeleniumWebControllerFactoryImpl implements WebControllerFactory {

		public WebController initialize(ApplicationContext context, WebController controller) {

			SeleniumWebController selController = (SeleniumWebController) controller;

			LOG.info("Selenium RC mode; connecting to a Selenium RC host");
			Selenium selenium = null;
			if (SteviaContext.getParam(BROWSER) == null || SteviaContext.getParam(BROWSER).compareTo("firefox") == 0
					|| SteviaContext.getParam(BROWSER).isEmpty()) {
				LOG.info("Using Firefox with selenium RC");
				selenium = new DefaultSelenium(SteviaContext.getParam(RC_HOST), Integer.parseInt(SteviaContext.getParam(RC_PORT)), FIREFOX,
						SteviaContext.getParam(TARGET_HOST_URL));
			} else if (SteviaContext.getParam(BROWSER).compareTo("chrome") == 0) {
				LOG.info("Using Chrome with selenium RC");
				selenium = new DefaultSelenium(SteviaContext.getParam(RC_HOST), Integer.parseInt(SteviaContext.getParam(RC_PORT)), CHROME,
						SteviaContext.getParam(TARGET_HOST_URL));
			} else if (SteviaContext.getParam(BROWSER).compareTo("iexplorer") == 0) {
				LOG.info("Using Chrome with selenium RC");
				selenium = new DefaultSelenium(SteviaContext.getParam(RC_HOST), Integer.parseInt(SteviaContext.getParam(RC_PORT)), IEXPLORER,
						SteviaContext.getParam(TARGET_HOST_URL));
			} else if (SteviaContext.getParam(BROWSER).compareTo("safari") == 0) {
				LOG.info("Using Safari with selenium RC");
				selenium = new DefaultSelenium(SteviaContext.getParam(RC_HOST), Integer.parseInt(SteviaContext.getParam(RC_PORT)), SAFARI,
						SteviaContext.getParam(TARGET_HOST_URL));
			} else if (SteviaContext.getParam(BROWSER).compareTo("opera") == 0) {
				LOG.info("Using Opera with selenium RC");
				selenium = new DefaultSelenium(SteviaContext.getParam(RC_HOST), Integer.parseInt(SteviaContext.getParam(RC_PORT)), OPERA,
						SteviaContext.getParam(TARGET_HOST_URL));
			} else {
				throw new IllegalArgumentException(WRONG_BROWSER_PARAMETER);
			}
			selenium.start();
			// selenium.windowMaximize();
			selenium.open("");

			selController.setSelenium(selenium);

			return controller;
		}
	}

    static class AppiumIOSControllerFactoryImpl implements WebControllerFactory {
        public WebController initialize(ApplicationContext context, WebController controller) {
            AppiumIOSWebController wdController = (AppiumIOSWebController) controller;
            WebDriver driver = null;

            File appDir = new File(SteviaContext.getParam(APPLICATION_PATH));
            File app = new File(appDir, SteviaContext.getParam(APPLICATION_NAME));
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(CapabilityType.BROWSER_NAME, SteviaContext.getParam(OPERATING_SYSTEM));
            capabilities.setCapability(CapabilityType.VERSION, SteviaContext.getParam(VERSION));
            capabilities.setCapability("deviceName", SteviaContext.getParam(DEVICE_NAME));
            capabilities.setCapability(CapabilityType.PLATFORM, SteviaContext.getParam(PLATFORM));
            capabilities.setCapability("app", app.getAbsolutePath());
            capabilities.setCapability("canRotate", true);

            Augmenter augmenter = new Augmenter();
            augmenter.addDriverAugmentation("canRotate", new AddRotatable());

            try {
                driver = augmenter.augment(new SwipeableWebDriver(new URL("http://" + SteviaContext.getParam(RC_HOST) + ":" + SteviaContext.getParam(RC_PORT)
                        + "/wd/hub"), capabilities));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            wdController.setDriver(driver);
            return wdController;
        }
    }

    public static class SwipeableWebDriver extends RemoteWebDriver implements HasTouchScreen {
        private RemoteTouchScreen touch;

        public SwipeableWebDriver(URL remoteAddress, Capabilities desiredCapabilities) {
            super(remoteAddress, desiredCapabilities);
            touch = new RemoteTouchScreen(getExecuteMethod());
        }

        public SwipeableWebDriver() {
        }

        public TouchScreen getTouch() {
            return touch;
        }
    }

    static class AppiumAndroidControllerFactoryImpl implements WebControllerFactory{
        public WebController initialize(ApplicationContext context, WebController controller) {

            AppiumAndroidWebController wdController = (AppiumAndroidWebController) controller;
            WebDriver driver = null;

            File appDir = new File(SteviaContext.getParam(APPLICATION_PATH));
            File app = new File(appDir, SteviaContext.getParam(APPLICATION_NAME));
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(CapabilityType.BROWSER_NAME, SteviaContext.getParam(OPERATING_SYSTEM));
            capabilities.setCapability(CapabilityType.VERSION, SteviaContext.getParam(VERSION));
            capabilities.setCapability(CapabilityType.PLATFORM, SteviaContext.getParam(PLATFORM));
            capabilities.setCapability("app-package", SteviaContext.getParam(APPLICATION_PACKAGE));
            capabilities.setCapability("app-activity", SteviaContext.getParam(APPLICATION_ACTIVITY));
            capabilities.setCapability("canRotate", true);

            if(!app.equals(appDir)){
                capabilities.setCapability("app", app.getAbsolutePath());
            }

            Augmenter augmenter = new Augmenter();
            augmenter.addDriverAugmentation("canRotate", new AddRotatable());

            try {
                driver = augmenter.augment(new SwipeableWebDriver(new URL("http://" + SteviaContext.getParam(RC_HOST) + ":" + SteviaContext.getParam(RC_PORT)
                        + "/wd/hub"), capabilities));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            wdController.setDriver(driver);
            return wdController;
        }
    }

}
