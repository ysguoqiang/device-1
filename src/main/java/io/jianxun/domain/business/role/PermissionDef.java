package io.jianxun.domain.business.role;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.jianxun.web.dto.CodeNameDto;

//操作定义
public enum PermissionDef {
	// 用户管理相关权限
	USER_PAGE("USERLIST", "列表", ModuleDef.SYS, DomainDef.SYS_USER), USER_CREATE("USERCREATE", "新增", ModuleDef.SYS,
			DomainDef.SYS_USER), USER_MODIFY("USERMODIFY", "修改", ModuleDef.SYS, DomainDef.SYS_USER), USER_REMOVE(
					"USERREMOVE", "删除", ModuleDef.SYS, DomainDef.SYS_USER), USER_CHANGEPASSWROD("USERCHANGEPASSWROD",
							"重置密码", ModuleDef.SYS, DomainDef.SYS_USER), USER_RESETPASSWROD("USERRESETPASSWROD", "修改密码",
									ModuleDef.SYS, DomainDef.SYS_USER),
	// 角色
	ROLE_PAGE("ROLELIST", "列表", ModuleDef.SYS, DomainDef.SYS_ROLE), ROLE_CREATE("ROLECREATE", "新增", ModuleDef.SYS,
			DomainDef.SYS_ROLE), ROLE_MODIFY("ROLEMODIFY", "修改", ModuleDef.SYS,
					DomainDef.SYS_ROLE), ROLE_REMOVE("ROLEREMOVE", "删除", ModuleDef.SYS, DomainDef.SYS_ROLE);
	// 操作代码
	private String code;
	// 描述
	private String name;
	// 模块
	private ModuleDef module;

	// 模型代码
	private DomainDef domain;

	private PermissionDef(String code, String name, ModuleDef module, DomainDef domain) {
		this.code = code;
		this.name = name;
		this.module = module;
		this.domain = domain;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ModuleDef getModule() {
		return module;
	}

	public void setModule(ModuleDef module) {
		this.module = module;
	}

	public DomainDef getDomain() {
		return domain;
	}

	public void setDomain(DomainDef domain) {
		this.domain = domain;
	}

	private static Map<String, PermissionDef> valueMaps = Maps.newTreeMap();
	private static Map<String, List<CodeNameDto>> persMap = Maps.newTreeMap();
	private static Map<String, List<String>> initMap = Maps.newTreeMap();
	private static List<String> permissionCodeList = Lists.newArrayList();

	static {
		for (PermissionDef def : PermissionDef.values()) {
			// 系统设置权限组
			if (def.getDomain().equals(ModuleDef.SYS) || def.getDomain().equals(ModuleDef.ORGANIZATION)) {
				initValue("sys", def);
			}
			valueMaps.put(def.code, def);
			if (persMap.containsKey(def.getDomain().getCode())) {
				List<CodeNameDto> v = persMap.get(def.getDomain().getCode());
				v.add(converToCodeName(def));
			} else
				persMap.put(def.getDomain().getCode(), Lists.newArrayList(converToCodeName(def)));
			permissionCodeList.add(def.getCode());
		}
	}

	// 根据操作代码获取权限定义
	public static PermissionDef parse(String code) {
		return valueMaps.get(code);
	}

	private static void initValue(String key, PermissionDef def) {
		if (initMap.get(key) == null)
			initMap.put(key, Lists.newArrayList(def.getCode()));
		else
			initMap.get(key).add(def.getCode());

	}

	public static List<String> getPerGroup(String key) {
		return initMap.get(key);
	}

	private static CodeNameDto converToCodeName(PermissionDef def) {
		CodeNameDto dto = new CodeNameDto();
		dto.setCode(def.getCode());
		dto.setName(def.getName());
		return dto;
	}

	public static Map<String, List<CodeNameDto>> getPermission() {
		return persMap;
	}

	public static List<String> getPermissionCodeList() {
		return permissionCodeList;
	}

	public static void setPermissionCodeList(List<String> permissionCodeList) {
		PermissionDef.permissionCodeList = permissionCodeList;
	}

	// 模块定义
	public enum ModuleDef {
		SYS("sys", "系统设置", 99), ORGANIZATION("org", "组织管理", 7);
		private String code;
		private String name;
		private Integer sortNum = 99;

		private ModuleDef(String code, String name, Integer sortNum) {
			this.code = code;
			this.name = name;
			this.sortNum = sortNum;
		}

		private static Map<String, ModuleDef> valueMaps = Maps.newHashMap();

		static {
			for (ModuleDef category : ModuleDef.values()) {
				valueMaps.put(category.code, category);
			}
		}

		public static ModuleDef parse(String code) {
			return valueMaps.get(code);
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getSortNum() {
			return sortNum;
		}

		public void setSortNum(Integer sortNum) {
			this.sortNum = sortNum;
		}

	}

	// 模型定义
	public enum DomainDef {
		// 系统配置
		SYS_USER("sys_user", "用户管理", 0), SYS_ROLE("sys_role", "角色管理", 10);
		private String code;
		private String name;
		private Integer sortNum = 99;

		private DomainDef(String code, String name, Integer sortNum) {
			this.code = code;
			this.name = name;
			this.sortNum = sortNum;
		}

		private static Map<String, DomainDef> valueMaps = Maps.newHashMap();
		private static List<CodeNameDto> v = Lists.newArrayList();

		static {
			for (DomainDef def : DomainDef.values()) {
				if (def.getCode().startsWith("requestform_"))
					continue;
				valueMaps.put(def.code, def);
				v.add(converToCodeName(def));
			}
		}

		private static CodeNameDto converToCodeName(DomainDef def) {
			CodeNameDto dto = new CodeNameDto();
			dto.setCode(def.getCode());
			dto.setName(def.getName());
			return dto;
		}

		public static DomainDef parse(String code) {
			return valueMaps.get(code);
		}

		public static List<CodeNameDto> getDomainDefs() {
			return v;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getSortNum() {
			return sortNum;
		}

		public void setSortNum(Integer sortNum) {
			this.sortNum = sortNum;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
