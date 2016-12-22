package com.exlyo.camerarestarter;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.exlyo.androidutils.billing.MyBillingManager;

import java.util.ArrayList;
import java.util.List;

public class CRBillingManager {
	private static final String BASE_64_ENCODED_PUBLIC_KEY = ""
		//
		+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgh/xaaHzS1FhSm62QWgmO3xQ57hlZk15hDxQ"
		+ "qYNN1+QNGvUMBOAqbf7AVoF5mdrrTHQD0t3OkNmmjZxiQSXsGJim5Hm61qRQw/YlNGTAAmnoLhcox1Zh"
		+ "qpbtUd+yucxix0NVyVh5YYtLWT/98qMRKrxFMaM963iVJt3xgSGJ78At9ghMQeWvgoAaY1bES5XQIl7i"
		+ "rFxfvR34bX6rLUTc/voHCe5JehacB+pGGfKw1c+Gjc7SoJBQ+a/cuffOyyAy5JgtwKcjHOviVmBbDNnd"
		+ "v1fju0dW9+35unw+qDQEXRh6KiW2h9pIYjQczDrLP2g6NmrFdcB2EfxOJmffL9OXvQIDAQAB";

	public static final String SKU_DONATE_1 = "donate_1";
	public static final String SKU_DONATE_2 = "donate_2";
	public static final String SKU_DONATE_3 = "donate_3";
	public static final String SKU_DONATE_5 = "donate_5";
	public static final String SKU_DONATE_10 = "donate_10";
	public static final String SKU_DONATE_20 = "donate_20";
	public static final String SKU_DONATE_50 = "donate_50";
	public static final String SKU_DONATE_100 = "donate_100";
	public static final String SKU_DONATE_200 = "donate_200";

	public static final List<String> ALL_SKU_LIST = new ArrayList<>();

	static {
		ALL_SKU_LIST.add(SKU_DONATE_1);
		ALL_SKU_LIST.add(SKU_DONATE_2);
		ALL_SKU_LIST.add(SKU_DONATE_3);
		ALL_SKU_LIST.add(SKU_DONATE_5);
		ALL_SKU_LIST.add(SKU_DONATE_10);
		ALL_SKU_LIST.add(SKU_DONATE_20);
		ALL_SKU_LIST.add(SKU_DONATE_50);
		ALL_SKU_LIST.add(SKU_DONATE_100);
		ALL_SKU_LIST.add(SKU_DONATE_200);
	}

	private static MyBillingManager bm = null;

	public static MyBillingManager get() {
		if (bm != null) {
			return bm;
		}
		synchronized (CRBillingManager.class) {
			if (bm == null) {
				bm = new MyBillingManager(BASE_64_ENCODED_PUBLIC_KEY, 77);
			}
		}

		return bm;
	}

	private static void actionPerformPurchase(final Activity _activity, final String _sku) {
		try {
			final MyBillingManager.MyPurchaseResult res = CRBillingManager.get().performPurchase(_activity, _sku);
			if (res.result.isSuccess()) {
				MainActivity.showToastMessage(_activity, _activity.getString(R.string.thank_you_for_your_donation));
			} else {
				MainActivity.showToastMessage(_activity, res.result.getMessage());
			}
		} catch (Throwable _throwable) {
			_throwable.printStackTrace();
		}
	}

	public static void actionDonate(final MainActivity _mainActivity) {
		final CharSequence[] items = new String[]{//
			_mainActivity.getString(R.string.donate_1),//
			_mainActivity.getString(R.string.donate_2),//
			_mainActivity.getString(R.string.donate_5),//
			_mainActivity.getString(R.string.donate_10),//
			_mainActivity.getString(R.string.donate_20),//
			_mainActivity.getString(R.string.donate_50), //
			_mainActivity.getString(R.string.donate_100), //
			_mainActivity.getString(R.string.donate_200),//
		};
		final Runnable[] actionList = new Runnable[]{//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_1);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_2);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_5);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_10);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_20);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_50);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_100);
				}
			},//
			new Runnable() {
				@Override
				public void run() {
					actionPerformPurchase(_mainActivity, CRBillingManager.SKU_DONATE_200);
				}
			},//
		};

		final Runnable[] selectedRunnable = new Runnable[]{null};

		AlertDialog.Builder builder =
			new AlertDialog.Builder(_mainActivity).setSingleChoiceItems(items, 2, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface _dialogInterface, final int _index) {
					selectedRunnable[0] = actionList[_index];
				}
			});
		builder.setTitle(R.string.choose_the_amount_to_donate);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface _dialogInterface, final int _i) {
				if (selectedRunnable[0] != null) {
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(final Void... _voids) {
							try {
								selectedRunnable[0].run();
							} catch (Throwable t) {
								t.printStackTrace();
							}
							return null;
						}
					}.execute();
				}
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
