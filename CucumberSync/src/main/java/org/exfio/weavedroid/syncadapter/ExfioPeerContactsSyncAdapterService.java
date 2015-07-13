package org.exfio.weavedroid.syncadapter;

import lombok.Getter;

import org.exfio.weavedroid.Constants;

public class ExfioPeerContactsSyncAdapterService extends ContactsSyncAdapterService {
	@Getter private static final String accountType = Constants.ACCOUNT_TYPE_EXFIOPEER;
}
