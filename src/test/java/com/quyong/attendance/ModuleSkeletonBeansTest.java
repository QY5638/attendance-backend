package com.quyong.attendance;

import com.quyong.attendance.module.auth.controller.AuthController;
import com.quyong.attendance.module.auth.service.impl.AuthServiceImpl;
import com.quyong.attendance.module.department.controller.DepartmentController;
import com.quyong.attendance.module.department.service.impl.DepartmentServiceImpl;
import com.quyong.attendance.module.device.controller.DeviceController;
import com.quyong.attendance.module.device.service.impl.DeviceServiceImpl;
import com.quyong.attendance.module.role.controller.RoleController;
import com.quyong.attendance.module.role.service.impl.RoleServiceImpl;
import com.quyong.attendance.module.user.controller.UserController;
import com.quyong.attendance.module.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class ModuleSkeletonBeansTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadCoreModuleControllersAndServices() {
        assertNotNull(applicationContext.getBean(AuthController.class));
        assertNotNull(applicationContext.getBean(AuthServiceImpl.class));
        assertNotNull(applicationContext.getBean(UserController.class));
        assertNotNull(applicationContext.getBean(UserServiceImpl.class));
        assertNotNull(applicationContext.getBean(DepartmentController.class));
        assertNotNull(applicationContext.getBean(DepartmentServiceImpl.class));
        assertNotNull(applicationContext.getBean(RoleController.class));
        assertNotNull(applicationContext.getBean(RoleServiceImpl.class));
        assertNotNull(applicationContext.getBean(DeviceController.class));
        assertNotNull(applicationContext.getBean(DeviceServiceImpl.class));
    }
}
