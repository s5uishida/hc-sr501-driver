package io.github.s5uishida.iot.device.hcsr501.driver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiGpioProvider;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.RaspiPinNumberingScheme;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/*
 * Refer to https://www.mpja.com/download/31227sc.pdf
 *
 * @author s5uishida
 *
 */
public class HCSR501Driver {
	private static final Logger LOG = LoggerFactory.getLogger(HCSR501Driver.class);

	private final Pin gpioPin;
	private final GpioController gpio;
	private final IHCSR501Handler hcsr501Handler;
	private final String logPrefix;

	private GpioPinDigitalInput diPin;

	private static final ConcurrentHashMap<String, HCSR501Driver> map = new ConcurrentHashMap<String, HCSR501Driver>();

	private final AtomicInteger useCount = new AtomicInteger(0);

	private HCSR501GpioPinListenerDigital hcsr501Listener;

	synchronized public static HCSR501Driver getInstance() {
		return getInstance(RaspiPin.GPIO_10, null);
	}

	synchronized public static HCSR501Driver getInstance(Pin gpioPin) {
		return getInstance(gpioPin, null);
	}

	synchronized public static HCSR501Driver getInstance(Pin gpioPin, IHCSR501Handler hcsr501Handler) {
		String key = getName(Objects.requireNonNull(gpioPin));
		HCSR501Driver hcsr501 = map.get(key);
		if (hcsr501 == null) {
			hcsr501 = new HCSR501Driver(gpioPin, hcsr501Handler);
			map.put(key, hcsr501);
		}
		return hcsr501;
	}

	private HCSR501Driver(Pin gpioPin, IHCSR501Handler hcsr501Handler) {
		if (gpioPin.equals(RaspiPin.GPIO_18) || gpioPin.equals(RaspiPin.GPIO_19) ||
				gpioPin.equals(RaspiPin.GPIO_12) || gpioPin.equals(RaspiPin.GPIO_13)) {
			this.gpioPin = gpioPin;
		} else {
			throw new IllegalArgumentException("The set " + getName(gpioPin) + " is not " +
					getName(RaspiPin.GPIO_18) + ", " +
					getName(RaspiPin.GPIO_19) + ", " +
					getName(RaspiPin.GPIO_12) + " or " +
					getName(RaspiPin.GPIO_13) + ".");
		}
		logPrefix = "[" + getName() + "] ";
		GpioFactory.setDefaultProvider(new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING));
		gpio = GpioFactory.getInstance();
		this.hcsr501Handler = hcsr501Handler;
	}

	synchronized public void open() {
		try {
			LOG.debug(logPrefix + "before - useCount:{}", useCount.get());
			if (useCount.compareAndSet(0, 1)) {
				diPin = gpio.provisionDigitalInputPin(gpioPin, PinPullResistance.PULL_DOWN);
				diPin.setShutdownOptions(true);
				hcsr501Listener = new HCSR501GpioPinListenerDigital(this);
				diPin.addListener(hcsr501Listener);
				LOG.info(logPrefix + "opened");
			}
		} finally {
			LOG.debug(logPrefix + "after - useCount:{}", useCount.get());
		}
	}

	synchronized public void close() {
		try {
			LOG.debug(logPrefix + "before - useCount:{}", useCount.get());
			if (useCount.compareAndSet(1, 0)) {
				diPin.removeAllListeners();
				gpio.unprovisionPin(diPin);
//				gpio.shutdown();
				LOG.info(logPrefix + "closed");
			}
		} finally {
			LOG.debug(logPrefix + "after - useCount:{}", useCount.get());
		}
	}

	public static String getName(Pin gpioPin) {
		return gpioPin.getName().replaceAll("\\s", "_");
	}

	public String getName() {
		return gpioPin.getName().replaceAll("\\s", "_");
	}

	public String getLogPrefix() {
		return logPrefix;
	}

	/******************************************************************************************************************
	 * Sample main
	 ******************************************************************************************************************/
	public static void main(String[] args) {
		HCSR501Driver hcsr501 = HCSR501Driver.getInstance(RaspiPin.GPIO_12, new MyHCSR501Handler());
		hcsr501.open();

//		if (hcsr501 != null) {
//			hcsr501.close();
//		}
	}

	class HCSR501GpioPinListenerDigital implements GpioPinListenerDigital {
		private final Logger LOG = LoggerFactory.getLogger(HCSR501GpioPinListenerDigital.class);

		private final HCSR501Driver hcsr501;

		public HCSR501GpioPinListenerDigital(HCSR501Driver hcsr501) {
			this.hcsr501 = hcsr501;
		}

		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			LOG.trace(hcsr501.getLogPrefix() + "{} -> {}", event.getPin(), event.getState());

			Date date = new Date();
			if (event.getState() == PinState.HIGH) {
				hcsr501.hcsr501Handler.handle(hcsr501.getName(), true, date);
			} else if (event.getState() == PinState.LOW) {
				hcsr501.hcsr501Handler.handle(hcsr501.getName(), false, date);
			}
		}
	}
}

/******************************************************************************************************************
 * Sample implementation of IHCSR501Handler interface
 ******************************************************************************************************************/
class MyHCSR501Handler implements IHCSR501Handler {
	private static final Logger LOG = LoggerFactory.getLogger(MyHCSR501Handler.class);

	private static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

	@Override
	public void handle(String pinName, boolean detect, Date date) {
		LOG.info("[{}] {} {}", pinName, detect, sdf.format(date));
	}
}
