package com.tvd12.ezyfoxserver.testing;

import com.tvd12.ezyfoxserver.constant.EzyConstant;
import com.tvd12.ezyfoxserver.delegate.EzyUserRemoveDelegate;
import com.tvd12.ezyfoxserver.entity.EzySimpleUser;

public class MyTestUser extends EzySimpleUser {
    private static final long serialVersionUID = 9150382549072410382L;
    
    @Override
    public EzyUserRemoveDelegate getRemoveDelegate() {
        return new EzyUserRemoveDelegate() {
            
            @Override
            public void onUserRemoved(EzyConstant reason) {
            }
        };
    }

}
