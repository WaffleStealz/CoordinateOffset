package com.jtprince.coordinateoffset.provider;

public abstract sealed class CoreOffsetProvider
    extends OffsetProvider
    permits ConstantOffsetProvider, PermissionOffsetProvider, RandomOffsetProvider, ZeroAtLocationOffsetProvider {

    public CoreOffsetProvider(String userDefinedProviderName) {
        super(userDefinedProviderName);
    }

    public abstract String getMetricsClassName();

    public abstract String getMetricsDetails();
}
