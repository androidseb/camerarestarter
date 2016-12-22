package com.exlyo.androidutils.billing;

import android.app.Activity;
import android.content.Intent;

import com.exlyo.androidutils.billing.util.IabHelper;
import com.exlyo.androidutils.billing.util.IabResult;
import com.exlyo.androidutils.billing.util.Inventory;
import com.exlyo.androidutils.billing.util.Purchase;
import com.exlyo.androidutils.billing.util.SkuDetails;

import java.util.ArrayList;
import java.util.List;

public class MyBillingManager {
	public static final class MyIAPItem {
		public final String sku;
		public final String description;
		public final String price;

		public MyIAPItem(final String _sku, final String _description, final String _price) {
			sku = _sku;
			description = _description;
			price = _price;
		}
	}

	private static final Object transactionsWaitMutex = new Object();
	private final String base64EncodedPublicKey;
	private final int purchaseActivityRequestCode;

	public MyBillingManager(final String _base64EncodedPublicKey, final int _purchaseActivityRequestCode) {
		base64EncodedPublicKey = _base64EncodedPublicKey;
		purchaseActivityRequestCode = _purchaseActivityRequestCode;
	}

	private static abstract class IAPTransaction<T> {
		private final Activity activity;
		private final MyBillingManager billingManager;

		public IAPTransaction(final Activity _activity, final MyBillingManager _billingManager) {
			activity = _activity;
			billingManager = _billingManager;
		}

		public final T performAndGetTransactionResult() throws Throwable {
			final IabHelper helper = new IabHelper(activity, billingManager.base64EncodedPublicKey);
			synchronized (transactionsWaitMutex) {
				final boolean[] setupFinished = new boolean[]{false};
				helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
					@Override
					public void onIabSetupFinished(IabResult result) {
						synchronized (transactionsWaitMutex) {
							setupFinished[0] = true;
							transactionsWaitMutex.notifyAll();
						}
					}
				});
				while (!setupFinished[0]) {
					transactionsWaitMutex.wait();
				}
			}

			T res = null;

			try {
				res = performTransaction(helper);
			} finally {
				helper.dispose();
			}

			return res;
		}

		protected abstract T performTransaction(final IabHelper _helper) throws Throwable;
	}

	public List<MyIAPItem> getPurchaseableItems(final Activity _activity, final List<String> _skuList) {
		try {
			final Inventory inventory = new IAPTransaction<Inventory>(_activity, this) {
				@Override
				protected Inventory performTransaction(final IabHelper _helper) throws Throwable {
					return _helper.queryInventory(true, _skuList);
				}
			}.performAndGetTransactionResult();

			final List<MyIAPItem> res = new ArrayList<>();
			for (final String sku : _skuList) {
				if (!inventory.hasDetails(sku) || inventory.hasPurchase(sku)) {
					continue;
				}
				final SkuDetails skuDetails = inventory.getSkuDetails(sku);
				res.add(new MyIAPItem(sku, skuDetails.getDescription(), skuDetails.getPrice()));
			}

			return res;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public List<String> getAllOwnedItemSkus(final Activity _activity) {
		try {
			final Inventory inventory = new IAPTransaction<Inventory>(_activity, this) {
				@Override
				protected Inventory performTransaction(final IabHelper _helper) throws Throwable {
					return _helper.queryInventory(false, new ArrayList<String>());
				}
			}.performAndGetTransactionResult();

			return inventory.getAllOwnedSkus();
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	private static IabHelper pendingIntentHelper = null;

	public void onActivityResult(final int _requestCode, final int _resultCode, final Intent _data) {
		if (_requestCode == purchaseActivityRequestCode) {
			final IabHelper helper = pendingIntentHelper;
			if (helper != null) {
				helper.handleActivityResult(_requestCode, _resultCode, _data);
			}
		}
	}

	public static final class MyPurchaseResult {
		public final Purchase purchase;
		public final IabResult result;

		public MyPurchaseResult(final Purchase _purchase, final IabResult _result) {
			purchase = _purchase;
			result = _result;
		}
	}

	public MyPurchaseResult performPurchase(final Activity _activity, final String _sku) throws Throwable {
		return new IAPTransaction<MyPurchaseResult>(_activity, this) {
			@Override
			protected MyPurchaseResult performTransaction(final IabHelper _helper) throws Throwable {
				final MyPurchaseResult[] res = new MyPurchaseResult[]{null};
				synchronized (transactionsWaitMutex) {
					pendingIntentHelper = _helper;
					_helper.launchPurchaseFlow(_activity, _sku, purchaseActivityRequestCode, new IabHelper.OnIabPurchaseFinishedListener() {
						@Override
						public void onIabPurchaseFinished(final IabResult result, final Purchase info) {
							res[0] = new MyPurchaseResult(info, result);
							synchronized (transactionsWaitMutex) {
								transactionsWaitMutex.notifyAll();
							}
						}
					});
					transactionsWaitMutex.wait();
					pendingIntentHelper = null;
				}
				return res[0];
			}
		}.performAndGetTransactionResult();
	}
}
