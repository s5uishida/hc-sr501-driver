package io.github.s5uishida.iot.device.hcsr501.driver;

import java.util.Date;

/*
 * @author s5uishida
 *
 */
public interface IHCSR501Handler {
	void handle(String pinName, boolean detect, Date date);
}
