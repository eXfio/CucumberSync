package org.exfio.csyncdroid.syncadapter;

import lombok.Getter;

import org.exfio.csyncdroid.Constants;

public class ExfioPeerContactsSyncAdapterService extends ContactsSyncAdapterService {
	@Getter private static final String accountType = Constants.ACCOUNT_TYPE_EXFIOPEER;
}
