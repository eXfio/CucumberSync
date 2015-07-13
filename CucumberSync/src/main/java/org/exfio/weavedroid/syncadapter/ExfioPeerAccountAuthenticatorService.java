package org.exfio.weavedroid.syncadapter;

import lombok.Getter;

import org.exfio.weavedroid.Constants;

public class ExfioPeerAccountAuthenticatorService extends AccountAuthenticatorService {
	@Getter private static final String accountType = Constants.ACCOUNT_TYPE_EXFIOPEER;
}
