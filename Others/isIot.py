def is_iot_app(apk):
    # 检查常见的物联网相关权限
    iot_permissions = [
        'android.permission.INTERNET',
        'android.permission.ACCESS_NETWORK_STATE',
        'android.permission.ACCESS_WIFI_STATE',
        'android.permission.CHANGE_WIFI_STATE',
        'android.permission.BLUETOOTH',
        'android.permission.BLUETOOTH_ADMIN',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.ACCESS_COARSE_LOCATION'
    ]
    
    permissions = apk.get_permissions()
    iot_permission_count = sum(1 for perm in iot_permissions if perm in permissions)
    
    # 检查活动和服务名称中是否包含物联网相关关键词
    iot_keywords = ['iot', 'smart', 'device', 'connect', 'sensor', 'control']
    activities = apk.get_activities()
    services = apk.get_services()
    
    keyword_count = sum(1 for item in activities + services 
                        for keyword in iot_keywords 
                        if keyword in item.lower())
    
    # 如果满足以下条件之一，认为可能是物联网应用：
    # 1. 包含至少3个物联网相关权限
    # 2. 活动或服务名称中包含至少2个物联网相关关键词
    return iot_permission_count >= 3 or keyword_count >= 2