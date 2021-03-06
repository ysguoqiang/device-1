package io.jianxun.service.business;

import org.apache.commons.lang3.StringUtils;

import com.querydsl.core.types.Predicate;

import io.jianxun.domain.business.Depart;
import io.jianxun.domain.business.Device;
import io.jianxun.domain.business.QSparePart;

/**
 * 仓库查询
 * 
 * @author Administrator
 *
 */
public class SparePartPredicates {

	private SparePartPredicates() {
	}

	// 仓库名查询
	public static Predicate namePredicate(String name) {
		return nameAndIdNotPredicate(name, null);
	}

	public static Predicate nameContainsPredicate(String name) {
		QSparePart sparePart = QSparePart.sparePart;
		return sparePart.name.containsIgnoreCase(name);
	}

	public static Predicate nameAndIdNotPredicate(String name, Long id) {
		QSparePart sparePart = QSparePart.sparePart;
		if (id == null)
			return sparePart.name.eq(name);
		return sparePart.id.ne(id).and(sparePart.name.eq(name));
	}

	public static Predicate departPredicate(Depart depart) {
		QSparePart sparePart = QSparePart.sparePart;
		return sparePart.depart.eq(depart);
	}

	public static Predicate departSubPredicate(Depart depart) {
		QSparePart sparePart = QSparePart.sparePart;
		if (StringUtils.isBlank(depart.getLevelCode()))
			return null;
		return sparePart.depart.levelCode.startsWith(depart.getLevelCode());
	}

	public static Predicate devicePredicate(Device device) {
		QSparePart sparePart = QSparePart.sparePart;
		return sparePart.device.eq(device);
	}

}
