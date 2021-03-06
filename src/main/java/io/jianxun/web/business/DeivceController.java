package io.jianxun.web.business;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;

import io.jianxun.domain.business.Depart;
import io.jianxun.domain.business.Device;
import io.jianxun.domain.business.DeviceDiscard;
import io.jianxun.domain.business.DeviceStatus;
import io.jianxun.domain.business.SparePart;
import io.jianxun.service.BusinessException;
import io.jianxun.service.LocaleMessageSourceService;
import io.jianxun.service.business.DepartService;
import io.jianxun.service.business.DeviceAdjustmentPredicates;
import io.jianxun.service.business.DeviceAdjustmentService;
import io.jianxun.service.business.DeviceCheckInfoPredicates;
import io.jianxun.service.business.DeviceCheckInfoService;
import io.jianxun.service.business.DeviceFaultPredicates;
import io.jianxun.service.business.DeviceFaultService;
import io.jianxun.service.business.DevicePredicates;
import io.jianxun.service.business.DeviceService;
import io.jianxun.service.business.DeviceStorageService;
import io.jianxun.service.business.DeviceTechnicalParamPredicates;
import io.jianxun.service.business.DeviceTechnicalParamService;
import io.jianxun.service.business.ProductionLinePredicates;
import io.jianxun.service.business.ProductionLineService;
import io.jianxun.service.business.SparePartMainTypeService;
import io.jianxun.service.business.SparePartPredicates;
import io.jianxun.service.business.SparePartService;
import io.jianxun.web.business.validator.DeviceValidator;
import io.jianxun.web.dto.ReturnDto;
import io.jianxun.web.dto.UploadPicReturnDto;
import io.jianxun.web.dto.ValueLabelDto;
import io.jianxun.web.utils.CurrentLoginInfo;
import io.jianxun.web.utils.Utils;

@Controller
@RequestMapping("device")
public class DeivceController {

	@InitBinder("device")
	public void initBinder(WebDataBinder webDataBinder) {
		webDataBinder.addValidators(deviceValidator);

	}

	@GetMapping(value = "tree")
	@PreAuthorize("hasAuthority('DEVICELIST')")
	public String tree(Model model, @RequestParam MultiValueMap<String, String> parameters) {
		try {
			model.addAttribute("tree", mapper.writeValueAsString(deviceService.getDeviceTree()));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new BusinessException(localeMessageSourceService.getMessage("depart.tree.error"));
		}
		return templatePrefix() + "tree";

	}

	/**
	 * 分页列表 支持 查询 分页 及 排序
	 */
	@RequestMapping(value = { "/page/{depart}" })
	@PreAuthorize("hasAuthority('DEVICELIST')")
	String page(@PathVariable("depart") Long departId, Model model,
			@QuerydslPredicate(root = Device.class) Predicate predicate,
			@PageableDefault(value = 20, sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam MultiValueMap<String, String> parameters) {
		Depart depart = this.departService.findActiveOne(departId);
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		// 查看仓库查询权限
		if (!currentLoginInfo.validateCurrentUserDepart(depart))
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notview"));
		Predicate searchPredicate = DevicePredicates.statusPredicate(null);
		Page<Device> page = null;
		if (predicate != null) {
			searchPredicate = ExpressionUtils.and(searchPredicate, predicate);
		}
		if (!depart.isRoot())
			searchPredicate = ExpressionUtils.and(searchPredicate, DevicePredicates.departSubPredicate(depart));
		page = deviceService.findActivePage(searchPredicate, pageable);
		util.addPageInfo(model, parameters, page);
		util.addSearchInfo(model, parameters);
		addParentDeviceInfo(model, depart);
		addUrl(model, "page/" + departId);
		model.addAttribute("createUrl", "device/create/" + depart.getId());
		addLineAndTypeInfo(model, depart);
		addCreateable(model);
		return templatePrefix() + Utils.PAGE_TEMPLATE_SUFFIX;
	}

	@RequestMapping(value = { "/page" })
	@PreAuthorize("hasAuthority('DEVICELIST')")
	String page(Model model, @QuerydslPredicate(root = Device.class) Predicate predicate,
			@PageableDefault(value = 20, sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam(name = "searchDepart", required = false) Long searchDepart,
			@RequestParam MultiValueMap<String, String> parameters) {
		Depart depart = this.currentLoginInfo.currentLoginUser().getDepart();
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		Predicate searchPredicate = ExpressionUtils.and(DevicePredicates.statusPredicate(null),
				DevicePredicates.departSubPredicate(depart));
		Page<Device> page = null;
		if (searchDepart != null) {
			Depart search = departService.findActiveOne(searchDepart);
			if (search != null)
				searchPredicate = ExpressionUtils.and(DevicePredicates.departSubPredicate(search), searchPredicate);
		}
		if (predicate != null)
			searchPredicate = ExpressionUtils.and(predicate, searchPredicate);
		page = deviceService.findActivePage(searchPredicate, pageable);
		util.addPageInfo(model, parameters, page);
		util.addSearchInfo(model, parameters);
		addUrl(model, "page/");
		model.addAttribute("createUrl", "device/create");
		addParentDeviceInfo(model, depart);
		addLineAndTypeInfo(model, depart);
		addCreateable(model);
		return templatePrefix() + Utils.PAGE_TEMPLATE_SUFFIX;
	}

	private void addCreateable(Model model) {
		model.addAttribute("createable", true);
	}

	private void addUrl(Model model, String url) {
		model.addAttribute("url", url);
	}

	private void addParentDeviceInfo(Model model, Depart depart) {
		model.addAttribute("depart", depart);
		List<Depart> departs = Lists.newArrayList(depart);
		departService.getSubDeparts(departs, depart);
		model.addAttribute("departs", departs);
	}

	/**
	 * 新增表单页面
	 */
	@GetMapping("create/{departId}")
	@PreAuthorize("hasAuthority('DEVICECREATE')")
	String createForm(@PathVariable("departId") Long departId, Model model,
			@RequestParam MultiValueMap<String, String> parameters) {
		Depart depart = departService.findActiveOne(departId);
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		model.addAttribute("device", new Device());
		model.addAttribute("departId", departId);
		addLineAndTypeInfo(model, depart);
		util.addCreateFormAction(model);
		return templatePrefix() + Utils.SAVE_TEMPLATE_SUFFIX;
	}

	/**
	 * 新增表单页面
	 */
	@GetMapping("create")
	@PreAuthorize("hasAuthority('DEVICECREATE')")
	String createForm(Model model, @RequestParam MultiValueMap<String, String> parameters) {
		Depart depart = this.currentLoginInfo.currentLoginUser().getDepart();
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		model.addAttribute("device", new Device());
		addParentDeviceInfo(model, depart);
		addLineAndTypeInfo(model, depart);
		util.addCreateFormAction(model);
		return templatePrefix() + Utils.SAVE_TEMPLATE_SUFFIX;
	}

	private void addLineAndTypeInfo(Model model, Depart depart) {
		if (depart.isRoot())
			model.addAttribute("lines", productionLineService.findActiveAll(new Sort("name")));
		else
			model.addAttribute("lines", productionLineService
					.findActiveAll(ProductionLinePredicates.departPredicate(depart), new Sort("name")));
		model.addAttribute("types", sparePartMainTypeService.findActiveAll(new Sort("name")));
	}

	/**
	 * 新增保存
	 * 
	 * @param device
	 * @param parameters
	 * @return
	 */
	@PostMapping("create")
	@PreAuthorize("hasAuthority('DEVICECREATE')")
	@ResponseBody
	ReturnDto createSave(@Valid Device device, @RequestParam MultiValueMap<String, String> parameters) {
		deviceService.save(device);
		return ReturnDto.ok(localeMessageSourceService.getMessage("device.save.successd"), true, "device-page");
	}

	/**
	 * 修改角色表单
	 * 
	 * @param id
	 * @param model
	 * @return
	 */
	@GetMapping(value = "/modify/{id}")
	@PreAuthorize("hasAuthority('DEVICEMODIFY')")
	public String modify(@PathVariable("id") Long id, Model model) {
		Device device = deviceService.findActiveOne(id);
		model.addAttribute("device", device);
		addParentDeviceInfo(model, this.currentLoginInfo.currentLoginUser().getDepart());
		util.addModifyFormAction(model);
		addLineAndTypeInfo(model, device.getDepart());
		return templatePrefix() + "form";

	}

	/**
	 * 修改角色保存
	 * 
	 * @param entity
	 * @param model
	 * @return
	 */
	@PostMapping(value = "/modify")
	@PreAuthorize("hasAuthority('DEVICEMODIFY')")
	@ResponseBody
	public ReturnDto modifySave(@Valid @ModelAttribute(name = "device") Device device, Model model) {
		if (!deviceService.modifiable(device))
			throw new BusinessException(localeMessageSourceService.getMessage("device.cannotmodify"));
		deviceService.save(device);
		return ReturnDto.ok(localeMessageSourceService.getMessage("device.save.successd"), true, "device-page");
	}

	@PostMapping("remove/{id}")
	@PreAuthorize("hasAuthority('DEVICEREMOVE')")
	@ResponseBody
	public ReturnDto remove(@PathVariable("id") Long id) {
		Device device = this.deviceService.findActiveOne(id);
		if (device == null)
			throw new BusinessException(localeMessageSourceService.getMessage("device.notfound"));
		if (!deviceService.modifiable(device))
			throw new BusinessException(localeMessageSourceService.getMessage("device.cannotmodify"));
		deviceService.delete(id);
		return ReturnDto.ok(localeMessageSourceService.getMessage("device.remove.successd"));
	}

	/**
	 * 验证名称是否重复
	 * 
	 * @param username
	 * @param id
	 * @return
	 */
	@RequestMapping("check/nameunique")
	@ResponseBody
	public String checkNameIsUnique(@RequestParam("name") String name, @RequestParam("depart.id") Long departId,
			@RequestParam("id") Long id) {
		Depart depart = this.departService.findActiveOne(departId);
		if (depart == null)
			return localeMessageSourceService.getMessage("depart.notfound");
		if (!this.deviceService.validateNameUnique(name, depart, id))
			return localeMessageSourceService.getMessage("device.name.isUsed", new Object[] { name });
		return "";
	}

	@RequestMapping("check/codeunique")
	@ResponseBody
	public String checkCodeIsUnique(@RequestParam("code") String code, @RequestParam("depart.id") Long departId,
			@RequestParam("id") Long id) {
		Depart depart = this.departService.findActiveOne(departId);
		if (depart == null)
			return localeMessageSourceService.getMessage("depart.notfound");
		if (!this.deviceService.validateCodeUnique(code, depart, id))
			return localeMessageSourceService.getMessage("device.code.isUsed", new Object[] { code });
		return "";
	}

	@PostMapping("pic/up")
	@ResponseBody
	public UploadPicReturnDto uploadPic(@RequestParam("file") MultipartFile file) {
		return new UploadPicReturnDto(200, "", deviceStorageService.store(UPLOAD_FOLDER_NAME, file));
	}

	@GetMapping("/pic/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = deviceStorageService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}

	@RequestMapping("export")
	@PreAuthorize("hasAuthority('DEVICELIST')")
	public void export(@RequestParam("d.id") Long departId, Model model,
			@QuerydslPredicate(root = Device.class) Predicate predicate, HttpServletResponse response)
			throws Exception {
		// 告诉浏览器用什么软件可以打开此文件
		response.setHeader("content-Type", "application/vnd.ms-excel");
		// 下载文件的默认名称
		response.setHeader("Content-Disposition", "attachment;filename=设备导出.xls");
		Depart depart = this.departService.findActiveOne(departId);
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		if (!currentLoginInfo.validateCurrentUserDepart(depart))
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notview"));
		Predicate searchPredicate = null;
		List<Device> list = null;
		if (predicate == null && depart.isRoot()) {
			list = this.deviceService.findActiveAll(new Sort("name", "id"));
		} else {
			searchPredicate = ExpressionUtils.and(DevicePredicates.departSubPredicate(depart), predicate);
			list = this.deviceService.findActiveAll(searchPredicate, new Sort("name", "id"));
		}
		Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), Device.class, list);
		workbook.write(response.getOutputStream());
	}

	/**
	 * 报废分页列表 支持 查询 分页 及 排序
	 */
	@RequestMapping(value = { "/discard/page" })
	@PreAuthorize("hasAuthority('DEVICEDISCARDPAGE')")
	String discardPage(Model model, @QuerydslPredicate(root = Device.class) Predicate predicate,
			@PageableDefault(sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam MultiValueMap<String, String> parameters) {
		Depart depart = this.departService.findActiveOne(this.currentLoginInfo.currentLoginUser().getDepart().getId());
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		Predicate searchPredicate = DevicePredicates.statusPredicate(DeviceStatus.DISCARD);
		if (!depart.isRoot())
			searchPredicate = ExpressionUtils.and(searchPredicate, DevicePredicates.departSubPredicate(depart));
		Page<Device> page = null;
		if (predicate != null)
			searchPredicate = ExpressionUtils.and(searchPredicate, predicate);
		page = deviceService.findActivePage(searchPredicate, pageable);
		util.addPageInfo(model, parameters, page);
		util.addSearchInfo(model, parameters);
		addParentDeviceInfo(model, depart);
		addLineAndTypeInfo(model, depart);
		addDiscardable(model);
		addUrl(model, "discard/page");
		return templatePrefix() + Utils.PAGE_TEMPLATE_SUFFIX;
	}

	private void addDiscardable(Model model) {
		model.addAttribute("discardable", true);

	}

	@RequestMapping("/discard/export")
	@PreAuthorize("hasAuthority('DEVICEDISCARDPAGE')")
	public void discardExport(Model model, @QuerydslPredicate(root = Device.class) Predicate predicate,
			HttpServletResponse response) throws Exception {
		// 告诉浏览器用什么软件可以打开此文件
		response.setHeader("content-Type", "application/vnd.ms-excel");
		// 下载文件的默认名称
		response.setHeader("Content-Disposition", "attachment;filename=设备导出.xls");
		Depart depart = this.departService.findActiveOne(this.currentLoginInfo.currentLoginUser().getDepart().getId());
		if (depart == null)
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notfound"));
		Predicate searchPredicate = DevicePredicates.statusPredicate(DeviceStatus.DISCARD);
		if (!depart.isRoot())
			searchPredicate = ExpressionUtils.and(searchPredicate, DevicePredicates.departSubPredicate(depart));
		List<Device> list = null;
		if (predicate != null)
			searchPredicate = ExpressionUtils.and(searchPredicate, predicate);
		list = deviceService.findActiveAll(searchPredicate, new Sort("name", "id"));
		Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), Device.class, list);
		workbook.write(response.getOutputStream());
	}

	@GetMapping(value = { "/discard/{id}" })
	@PreAuthorize("hasAuthority('DEVICEDISCARD')")
	String discardForm(@PathVariable("id") Long id, Model model) {
		Device device = this.deviceService.findActiveOne(id);
		if (device == null)
			throw new BusinessException(localeMessageSourceService.getMessage("device.notfound"));
		DeviceDiscard discard = new DeviceDiscard();
		discard.setDevice(device);
		model.addAttribute("discard", discard);
		return templatePrefix() + "discard";
	}

	@PostMapping(value = { "/discard" })
	@PreAuthorize("hasAuthority('DEVICEDISCARD')")
	@ResponseBody
	ReturnDto discard(DeviceDiscard discard) {
		Device device = this.deviceService.findActiveOne(discard.getDevice().getId());
		if (device == null)
			throw new BusinessException(localeMessageSourceService.getMessage("device.notfound"));
		discard.setDevice(device);
		this.deviceService.discard(discard);
		return ReturnDto.ok(localeMessageSourceService.getMessage("device.discard.successd"));
	}

	@RequestMapping(value = "/search")
	@ResponseBody
	public List<ValueLabelDto> getParentDepartStock(@RequestParam("term") String name, Model model) {
		Depart depart = this.departService.findActiveOne(this.currentLoginInfo.currentLoginUser().getDepart().getId());
		if (depart == null)
			throw new BusinessException("获取机构信息失败,无法获取对应设备");
		if (StringUtils.isBlank(name))
			return Lists.newArrayList();
		List<Device> parts = deviceService.findActiveAll(ExpressionUtils
				.and(DevicePredicates.departSubPredicate(depart), DevicePredicates.nameContainsPredicate(name)),
				new Sort("name"));
		return getDto(parts);

	}

	// 设备相关备件列表
	@RequestMapping("parts/{id}")
	@PreAuthorize("hasAuthority('SPAREPARTLIST')")
	public String spareparts(@PathVariable("id") Long id, Model model,
			@QuerydslPredicate(root = SparePart.class) Predicate predicate,
			@PageableDefault(sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam MultiValueMap<String, String> parameters) {
		Device device = this.deviceService.findActiveOne(id);
		if (device == null)
			throw new BusinessException(localeMessageSourceService.getMessage("dvice.notfound"));
		Predicate searchPredicate = ExpressionUtils.and(SparePartPredicates.devicePredicate(device),
				SparePartPredicates.departSubPredicate(device.getDepart()));
		Page<SparePart> page = null;
		if (predicate != null) {
			searchPredicate = ExpressionUtils.and(searchPredicate, predicate);
		}
		page = sparePartService.findActivePage(searchPredicate, pageable);

		// 计算库存
		sparePartService.getStock(page.getContent());
		util.addPageInfo(model, parameters, page);
		util.addSearchInfo(model, parameters);
		model.addAttribute("deviceId", id);
		return templatePrefix() + "parts";
	}

	@GetMapping("detail/{id}")
	public String detail(@PathVariable("id") Long id, Model model) {
		Device device = deviceService.findActiveOne(id);
		if (device == null)
			throw new BusinessException(localeMessageSourceService.getMessage("dvice.notfound"));
		if (!currentLoginInfo.validateCurrentUserDepart(device.getDepart()))
			throw new BusinessException(localeMessageSourceService.getMessage("depart.notview"));
		model.addAttribute("device", device);
		// 附属设备技术参数
		model.addAttribute("tps", deviceTechnicalParamService
				.findActiveAll(DeviceTechnicalParamPredicates.devicePredicate(device), new Sort("id")));
		// 调整履历
		model.addAttribute("ads", deviceAdjustmentService
				.findActiveAll(DeviceAdjustmentPredicates.devicePredicate(device), new Sort("id")));
		// 检修履历
		model.addAttribute("cs", deviceCheckInfoService.findActiveAll(DeviceCheckInfoPredicates.devicePredicate(device),
				new Sort("id")));
		// 事故履历
		model.addAttribute("fs",
				deviceFaultService.findActiveAll(DeviceFaultPredicates.devicePredicate(device), new Sort("id")));
		return templatePrefix() + "detail";
	}

	@ModelAttribute(name = "device")
	public void getMode(@RequestParam(value = "id", defaultValue = "-1") Long id, Model model) {
		if (id != null && id != -1L) {
			Device device = deviceService.findOne(id);
			if (device != null)
				model.addAttribute("device", device);
		}
	}

	private String templatePrefix() {
		return "device/";
	}

	private List<ValueLabelDto> getDto(List<Device> devices) {
		List<ValueLabelDto> vls = Lists.newArrayList();
		for (Device device : devices) {
			ValueLabelDto d = new ValueLabelDto();
			d.setLabel(device.toString());
			d.setValue(device.getId().toString());
			vls.add(d);
		}
		return vls;
	}

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private DeviceService deviceService;
	@Autowired
	private DepartService departService;
	@Autowired
	private LocaleMessageSourceService localeMessageSourceService;
	@Autowired
	private DeviceStorageService deviceStorageService;
	@Autowired
	private ProductionLineService productionLineService;
	@Autowired
	private SparePartMainTypeService sparePartMainTypeService;
	@Autowired
	private SparePartService sparePartService;
	@Autowired
	private DeviceTechnicalParamService deviceTechnicalParamService;
	@Autowired
	private DeviceAdjustmentService deviceAdjustmentService;
	@Autowired
	private DeviceCheckInfoService deviceCheckInfoService;
	@Autowired
	private DeviceFaultService deviceFaultService;

	@Autowired
	private Utils util;
	@Autowired
	private CurrentLoginInfo currentLoginInfo;
	@Autowired
	private DeviceValidator deviceValidator;
	private static final String UPLOAD_FOLDER_NAME = "shebei";

}
