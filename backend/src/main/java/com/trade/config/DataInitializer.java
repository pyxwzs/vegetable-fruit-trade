package com.trade.config;

import com.trade.entity.Permission;
import com.trade.entity.Role;
import com.trade.entity.User;
import com.trade.repository.PermissionRepository;
import com.trade.repository.RoleRepository;
import com.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("检查并初始化基础数据...");
        runStep("创建管理员账户", this::ensureAdminUser);
        runStep("创建默认角色", this::createDefaultRoles);
        runStep("补充细粒度权限", this::ensureFinancePermissionsExist);
        runStep("同步管理员全量权限", this::syncAdminRoleWithAllCatalogPermissions);
        runStep("同步财务角色权限", this::mergeFinanceRoleStandardPermissions);
        runStep("同步采购/销售角色权限", this::mergePurchaserAndSalesmanRoleStandardPermissions);
        runStep("同步仓管员角色权限", this::mergeWarehouseKeeperRoleStandardPermissions);
        log.info("基础数据初始化完成");
    }

    /** 每步独立执行，失败仅打印警告不中断启动 */
    private void runStep(String name, Runnable step) {
        try {
            step.run();
        } catch (Exception e) {
            log.warn("DataInitializer 步骤 [{}] 执行失败（下次启动时重试）: {}", name, e.getMessage());
            log.debug("DataInitializer 步骤 [{}] 详细异常", name, e);
        }
    }

    @Transactional
    public void ensureAdminUser() {
        if (!userRepository.findByUsername("admin").isPresent()) {
            log.info("创建默认管理员账户");
            Permission allPermission = createPermissionIfNotExists("ALL", "所有权限", "system", "*");
            Role adminRole = createRoleIfNotExists("ADMIN", "系统管理员", new HashSet<>(Arrays.asList(allPermission)));
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setRealName("系统管理员");
            admin.setStatus(User.UserStatus.ENABLED);
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
            userRepository.save(admin);
            log.info("管理员账户创建成功 - 用户名: admin, 密码: admin123");
        } else {
            log.info("管理员账户已存在");
        }
    }

    @Transactional
    public void ensureFinancePermissionsExist() {
        createPermissionIfNotExists("purchase:pay", "采购付款", "purchase", "pay");
        createPermissionIfNotExists("sales:collect", "销售收款", "sales", "collect");
    }

    @Transactional
    public void syncAdminRoleWithAllCatalogPermissions() {
        roleRepository.findByName("ADMIN").ifPresent(role -> {
            List<Permission> all = permissionRepository.findAll();
            if (all.isEmpty()) {
                return;
            }
            role.setPermissions(new HashSet<>(all));
            roleRepository.save(role);
            log.info("ADMIN 角色已同步权限表中的全部权限，共 {} 条", all.size());
        });
    }

    /**
     * 仓管员：库存直接操作（入库/出库/盘库/调拨）及商品查阅、采购/销售订单查看（需要确认收货/发货）。
     */
    @Transactional
    public void mergeWarehouseKeeperRoleStandardPermissions() {
        mergeRolePermissionsByCodes("WAREHOUSE_KEEPER",
                Set.of("inventory:view", "inventory:inbound", "inventory:outbound",
                        "product:view", "product:create", "product:update",
                        "purchase:view", "sales:view"));
    }

    /**
     * 采购员 / 销售员：至少具备各自模块的查看、创建、审核（含提交采购/销售退货，与 {@code PurchaseController}/{@code SalesController} 上
     * {@code purchase:create|approve}、{@code sales:create|approve} 一致）。
     * 同时补充商品查看和库存查看权限，支持选品与库存查询。
     */
    @Transactional
    public void mergePurchaserAndSalesmanRoleStandardPermissions() {
        mergeRolePermissionsByCodes("PURCHASER",
                Set.of("purchase:view", "purchase:create", "purchase:approve",
                        "product:view", "inventory:view"));
        mergeRolePermissionsByCodes("SALESMAN",
                Set.of("sales:view", "sales:create", "sales:approve",
                        "product:view", "inventory:view"));
    }

    private void mergeRolePermissionsByCodes(String roleName, Set<String> codes) {
        roleRepository.findByName(roleName).ifPresent(role -> {
            Set<Permission> extra = permissionRepository.findAll().stream()
                    .filter(p -> codes.contains(p.getCode()))
                    .collect(Collectors.toSet());
            if (extra.isEmpty()) {
                return;
            }
            Set<Permission> merged = new HashSet<>(role.getPermissions());
            merged.addAll(extra);
            role.setPermissions(merged);
            roleRepository.save(role);
            log.info("{} 角色已合并标准业务权限，当前共 {} 条", roleName, merged.size());
        });
    }

    @Transactional
    public void mergeFinanceRoleStandardPermissions() {
        roleRepository.findByName("FINANCE").ifPresent(role -> {
            Set<String> need = Set.of("purchase:view", "purchase:pay", "sales:view", "sales:collect");
            Set<Permission> extra = permissionRepository.findAll().stream()
                    .filter(p -> need.contains(p.getCode()))
                    .collect(Collectors.toSet());
            if (extra.isEmpty()) {
                return;
            }
            Set<Permission> merged = new HashSet<>(role.getPermissions());
            merged.addAll(extra);
            role.setPermissions(merged);
            roleRepository.save(role);
            log.info("FINANCE 角色已合并标准财务权限，当前共 {} 条", merged.size());
        });
    }

    private Permission createPermissionIfNotExists(String code, String name, String module, String action) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setCode(code);
                    permission.setName(name);
                    permission.setModule(module);
                    permission.setAction(action);
                    return permissionRepository.save(permission);
                });
    }

    private Role createRoleIfNotExists(String name, String description, HashSet<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    role.setPermissions(permissions);
                    return roleRepository.save(role);
                });
    }

    @Transactional
    public void createDefaultRoles() {
        String[][] roles = {
                {"PURCHASER", "采购员"},
                {"WAREHOUSE_KEEPER", "仓管员"},
                {"SALESMAN", "销售员"},
                {"FINANCE", "财务员"}
        };

        for (String[] roleInfo : roles) {
            if (!roleRepository.findByName(roleInfo[0]).isPresent()) {
                Role role = new Role();
                role.setName(roleInfo[0]);
                role.setDescription(roleInfo[1]);
                role.setPermissions(new HashSet<>());
                roleRepository.save(role);
                log.info("创建角色: {}", roleInfo[0]);
            }
        }
    }
}