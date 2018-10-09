
/*
 * Copyright 2012. Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:  The above
 * copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import java.util.Calendar;
import java.io.*;
import java.util.Scanner;

public class GettingIntradayBarData {

	public static void main(String[] args) throws Exception {
		GettingIntradayBarData example = new GettingIntradayBarData();
		example.run(args);
		System.out.println("Press ENTER to quit");
		System.in.read();
	}

	private Calendar getPreviousTradingStartDate(Calendar startDate) {
		if (startDate.get(Calendar.MONTH) != Calendar.JANUARY) {
			startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH) - 1, 1);
		} else {
			startDate.set(startDate.get(Calendar.YEAR) - 1, Calendar.DECEMBER, 1);
		}
		return startDate;
	}

	private Calendar getPreviousTradingEndDate(Calendar startDate) {
		if (startDate.get(Calendar.MONTH) == Calendar.FEBRUARY) {
			if (startDate.get(Calendar.YEAR) % 4 == 0) {
				startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), 29);
			} else {
				startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), 28);
			}
		} else if (startDate.get(Calendar.MONTH) == Calendar.APRIL || startDate.get(Calendar.MONTH) == Calendar.JUNE
				|| startDate.get(Calendar.MONTH) == Calendar.SEPTEMBER
				|| startDate.get(Calendar.MONTH) == Calendar.NOVEMBER) {
			startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), 30);
		} else {
			startDate.set(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), 31);
		}
		return startDate;
	}

	private void run(String[] args) throws Exception {
		String serverHost = "localhost";
		int serverPort = 8194;

		boolean isEmpty = false;
		boolean isInstance = true;
		Calendar startDate = null;
		Calendar endDate = null;

		String inputSecurity = null;
		String inputInterval = null;

		while (!isEmpty) {

		SessionOptions sessionOptions = new SessionOptions();
		sessionOptions.setServerHost(serverHost);
		sessionOptions.setServerPort(serverPort);

		System.out.println("Connecting to " + serverHost + ":" + serverPort);
		Session session = new Session(sessionOptions);
		if (!session.start()) {
			System.err.println("Failed to start session.");
			return;
		}
		if (!session.openService("//blp/refdata")) {
			System.err.println("Failed to open //blp/refdata");
			return;
		}
		Service refDataService = session.getService("//blp/refdata");
		Request request = refDataService.createRequest("IntradayBarRequest");

		// Scanner Input
		if(isInstance){
			Scanner scan = new Scanner(System.in);
			System.out.print("Please enter the Security Name: ");
			inputSecurity = new String(scan.nextLine());
			System.out.print("Please enter the Interval(min): ");
			inputInterval = new String(scan.nextLine());
		}

		request.set("security", inputSecurity);
		request.set("eventType", "TRADE");
		request.set("interval", inputInterval); // bar interval in minutes

		

			if (isInstance) {
				startDate = Calendar.getInstance();
				request.set("startDateTime", new Datetime(Calendar.getInstance().get(Calendar.YEAR),
						Calendar.getInstance().get(Calendar.MONTH) + 1, 1, 0, 0, 0, 0));
				request.set("endDateTime",
						new Datetime(Calendar.getInstance().get(Calendar.YEAR),
								Calendar.getInstance().get(Calendar.MONTH) + 1,
								Calendar.getInstance().get(Calendar.DAY_OF_MONTH), 23, 59, 0, 0));
				isInstance = false;
			} else {
				startDate = getPreviousTradingStartDate(startDate);
				endDate = getPreviousTradingEndDate(startDate);
				request.set("startDateTime", new Datetime(startDate.get(Calendar.YEAR),
						startDate.get(Calendar.MONTH) + 1, 1, 0, 0, 0, 0));
				request.set("endDateTime", new Datetime(endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH) + 1,
						endDate.get(Calendar.DAY_OF_MONTH), 23, 59, 0, 0));
			}

			// File Output
			StringBuilder fileName = new StringBuilder();
			int year = startDate.get(Calendar.YEAR);
			int month = startDate.get(Calendar.MONTH) + 1;
			int day = startDate.get(Calendar.DAY_OF_MONTH);
			
			fileName.append(inputSecurity);
			fileName.append("-");
			fileName.append(inputInterval);
			fileName.append("min-");
			fileName.append(Integer.toString(year));
			if(month < 10){
				fileName.append("0");
			}
			fileName.append(Integer.toString(month));
			fileName.append(".txt");
			PrintWriter out = new PrintWriter(fileName.toString());

			System.out.println("Sending Request: " + request);
			session.sendRequest(request, null);

			int i = 0;
			while (true) {
				Event event = session.nextEvent();
				MessageIterator msgIter = event.messageIterator();
				while (msgIter.hasNext()) {
					Message msg = msgIter.next();
					System.out.println(msg);
					if (i == 3) {
						String temp = msg.toString();
						if (temp.indexOf("time") >= 0) {
							out.println(msg);
						} else {
							isEmpty = true;
						}
					}
					i++;
				}
				if (event.eventType() == Event.EventType.RESPONSE || isEmpty) {
					break;
				}
			}
			out.close();
		}
	}
}
