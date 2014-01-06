/*
 * Copyright (C) 2007-2013 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.library;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;

import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.core.library.ZLibrary;
import org.geometerplus.zlibrary.core.options.ZLBooleanOption;
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption;

import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.fbreader.fbreader.ActionCode;

public final class ZLAndroidLibrary extends ZLibrary {
	public final ZLBooleanOption ShowStatusBarOption = new ZLBooleanOption("LookNFeel", "ShowStatusBar", hasNoHardwareMenuButton());
	public final ZLIntegerRangeOption BatteryLevelToTurnScreenOffOption = new ZLIntegerRangeOption("LookNFeel", "BatteryLevelToTurnScreenOff", 0, 100, 50);
	public final ZLBooleanOption DontTurnScreenOffDuringChargingOption = new ZLBooleanOption("LookNFeel", "DontTurnScreenOffDuringCharging", true);
	public final ZLIntegerRangeOption ScreenBrightnessLevelOption = new ZLIntegerRangeOption("LookNFeel", "ScreenBrightnessLevel", 0, 100, 0);
	public final ZLBooleanOption DisableButtonLightsOption = new ZLBooleanOption("LookNFeel", "DisableButtonLights", !hasButtonLightsBug());
	public final ZLBooleanOption EinkFastRefreshOption = new ZLBooleanOption("LookNFeel", "EinkFastRefresh", isEinkFastRefreshSupported());
	public final ZLIntegerRangeOption EinkUpdateIntervalOption = new ZLIntegerRangeOption("LookNFeel", "EinkUpdateInterval", 0, 20, 10);

	public static enum Device {
		GENERIC,
		YOTA_PHONE,
		KINDLE_FIRE,
		NOOK,
		NOOK12,
		EKEN_M001,
		PAN_DIGITAL,
		SAMSUNG_GT_S5830
	}

	private Device myDevice;

	public Device getDevice() {
		if (myDevice == null) {
			final String KINDLE_MODEL_REGEXP = ".*kindle(\\s+)fire.*";
			if ("YotaPhone".equals(Build.BRAND)) {
				myDevice = Device.YOTA_PHONE;
			} else if ("GT-S5830".equals(Build.MODEL)) {
				myDevice = Device.SAMSUNG_GT_S5830;
			} else if (Build.MODEL != null && Build.MODEL.toLowerCase().matches(KINDLE_MODEL_REGEXP)) {
				myDevice = Device.KINDLE_FIRE;
			} else if (Build.DISPLAY != null && Build.DISPLAY.contains("simenxie")) {
				myDevice = Device.EKEN_M001;
			} else if ("PD_Novel".equals(Build.MODEL)) {
				myDevice = Device.PAN_DIGITAL;
			} else {
				String MANUFACTURER = Build.MANUFACTURER;
				String MODEL = Build.MODEL;
				String DEVICE = Build.DEVICE;
				boolean nook = "barnesandnoble".equals(MANUFACTURER.toLowerCase()) && ("NOOK".equals(MODEL) || "BNRV350".equals(MODEL) || "BNRV300".equals(MODEL)) && "zoom2".equals(DEVICE.toLowerCase());
				boolean nook12 = nook && ("1.2.0".equals(Build.VERSION.INCREMENTAL) || "1.2.1".equals(Build.VERSION.INCREMENTAL));
				if (nook) {
					if (nook12) {
						myDevice = Device.NOOK12;
					} else {
						myDevice = Device.NOOK;
					}
				} else {
					myDevice = Device.GENERIC;
				}
			}
		}
		return myDevice;
	}

	private static List<Device> EinkDevices = Arrays.asList(Device.NOOK, Device.NOOK12);
	private static List<Device> EinkFastRefreshDevices = Arrays.asList(Device.NOOK, Device.NOOK12);
	private static List<Device> ButtonLightsBugDevices = Arrays.asList(Device.SAMSUNG_GT_S5830);
	private static List<Device> NoHardwareMenuButtonDevices = Arrays.asList(Device.EKEN_M001, Device.PAN_DIGITAL);

	private boolean hasNoHardwareMenuButton() {
		return NoHardwareMenuButtonDevices.contains(myDevice);
	}

	public boolean isKindleFire() {
		return myDevice == Device.KINDLE_FIRE;
	}

	public boolean hasButtonLightsBug() {
		return ButtonLightsBugDevices.contains(myDevice);
	}

	@Override
	public boolean isEink() {
		return EinkDevices.contains(myDevice);
	}

	@Override
	public boolean isEinkFastRefreshSupported() {
		return EinkFastRefreshDevices.contains(myDevice);
	}

	@Override
	public void initSpecificKeys(ZLKeyBindings b) {
		if (myDevice == Device.NOOK || myDevice == Device.NOOK12) {
			b.bindKey(92, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
			b.bindKey(94, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
			b.bindKey(93, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
			b.bindKey(95, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
		}
	}

	private FBReader myActivity;
	private final Application myApplication;

	ZLAndroidLibrary(Application application) {
		myApplication = application;
	}

	public void setActivity(FBReader activity) {
		myActivity = activity;
	}

	public void finish() {
		if (myActivity != null && !myActivity.isFinishing()) {
			myActivity.finish();
		}
	}

	public AssetManager getAssets() {
		return myApplication.getAssets();
	}

	@Override
	public ZLResourceFile createResourceFile(String path) {
		return new AndroidAssetsFile(path);
	}

	@Override
	public ZLResourceFile createResourceFile(ZLResourceFile parent, String name) {
		return new AndroidAssetsFile((AndroidAssetsFile)parent, name);
	}

	@Override
	public String getVersionName() {
		try {
			final PackageInfo info =
				myApplication.getPackageManager().getPackageInfo(myApplication.getPackageName(), 0);
			return info.versionName;
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public String getFullVersionName() {
		try {
			final PackageInfo info =
				myApplication.getPackageManager().getPackageInfo(myApplication.getPackageName(), 0);
			return info.versionName + " (" + info.versionCode + ")";
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public String getCurrentTimeString() {
		return DateFormat.getTimeFormat(myApplication.getApplicationContext()).format(new Date());
	}

	@Override
	public void setScreenBrightness(int percent) {
		if (myActivity != null) {
			myActivity.setScreenBrightness(percent);
		}
	}

	@Override
	public int getScreenBrightness() {
		return (myActivity != null) ? myActivity.getScreenBrightness() : 0;
	}

	private DisplayMetrics myMetrics;
	private DisplayMetrics getMetrics() {
		if (myMetrics == null) {
			if (myActivity == null) {
				return null;
			}
			myMetrics = new DisplayMetrics();
			myActivity.getWindowManager().getDefaultDisplay().getMetrics(myMetrics);
		}
		return myMetrics;
	}

	@Override
	public int getDisplayDPI() {
		final DisplayMetrics metrics = getMetrics();
		return metrics == null ? 0 : (int)(160 * metrics.density);
	}

	@Override
	public int getWidthInPixels() {
		final DisplayMetrics metrics = getMetrics();
		return metrics == null ? 0 : metrics.widthPixels;
	}

	@Override
	public int getHeightInPixels() {
		final DisplayMetrics metrics = getMetrics();
		return metrics == null ? 0 : metrics.heightPixels;
	}

	@Override
	public List<String> defaultLanguageCodes() {
		final TreeSet<String> set = new TreeSet<String>();
		set.add(Locale.getDefault().getLanguage());
		final TelephonyManager manager = (TelephonyManager)myApplication.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager != null) {
			final String country0 = manager.getSimCountryIso().toLowerCase();
			final String country1 = manager.getNetworkCountryIso().toLowerCase();
			for (Locale locale : Locale.getAvailableLocales()) {
				final String country = locale.getCountry().toLowerCase();
				if (country != null && country.length() > 0 &&
					(country.equals(country0) || country.equals(country1))) {
					set.add(locale.getLanguage());
				}
			}
			if ("ru".equals(country0) || "ru".equals(country1)) {
				set.add("ru");
			} else if ("by".equals(country0) || "by".equals(country1)) {
				set.add("ru");
			} else if ("ua".equals(country0) || "ua".equals(country1)) {
				set.add("ru");
			}
		}
		set.add("multi");
		return new ArrayList<String>(set);
	}

	@Override
	public boolean supportsAllOrientations() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	private final class AndroidAssetsFile extends ZLResourceFile {
		private final AndroidAssetsFile myParent;

		AndroidAssetsFile(AndroidAssetsFile parent, String name) {
			super(parent.getPath().length() == 0 ? name : parent.getPath() + '/' + name);
			myParent = parent;
		}

		AndroidAssetsFile(String path) {
			super(path);
			if (path.length() == 0) {
				myParent = null;
			} else {
				final int index = path.lastIndexOf('/');
				myParent = new AndroidAssetsFile(index >= 0 ? path.substring(0, path.lastIndexOf('/')) : "");
			}
		}

		@Override
		protected List<ZLFile> directoryEntries() {
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					ArrayList<ZLFile> files = new ArrayList<ZLFile>(names.length);
					for (String n : names) {
						files.add(new AndroidAssetsFile(this, n));
					}
					return files;
				}
			} catch (IOException e) {
			}
			return Collections.emptyList();
		}

		@Override
		public boolean isDirectory() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream == null) {
					return true;
				}
				stream.close();
				return false;
			} catch (IOException e) {
				return true;
			}
		}

		@Override
		public boolean exists() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream != null) {
					stream.close();
					// file exists
					return true;
				}
			} catch (IOException e) {
			}
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					// directory exists
					return true;
				}
			} catch (IOException e) {
			}
			return false;
		}

		private long mySize = -1;
		@Override
		public long size() {
			if (mySize == -1) {
				mySize = sizeInternal();
			}
			return mySize;
		}

		private long sizeInternal() {
			try {
				AssetFileDescriptor descriptor = myApplication.getAssets().openFd(getPath());
				// for some files (archives, crt) descriptor cannot be opened
				if (descriptor == null) {
					return sizeSlow();
				}
				long length = descriptor.getLength();
				descriptor.close();
				return length;
			} catch (IOException e) {
				return sizeSlow();
			}
		}

		private long sizeSlow() {
			try {
				final InputStream stream = getInputStream();
				if (stream == null) {
					return 0;
				}
				long size = 0;
				final long step = 1024 * 1024;
				while (true) {
					// TODO: does skip work as expected for these files?
					long offset = stream.skip(step);
					size += offset;
					if (offset < step) {
						break;
					}
				}
				return size;
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return myApplication.getAssets().open(getPath());
		}

		@Override
		public ZLFile getParent() {
			return myParent;
		}
	}
}
