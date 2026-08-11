package Npl.newSth.Type;

import mindustry.gen.UnitEntity;

/**
 * 多节段虫子单位的「段身」实体（骨架版）。
 *
 * <p>当前只做最小实现：让 {@link FedUnitEntity#register} 能拿到 classId、
 * 让 {@code SegmentUnitEntity::create} 能作为 UnitType 的 constructor 使用。
 * 后续要补的段身跟随逻辑（链式追随头部位置）先留空。
 */
public class SegmentUnitEntity extends UnitEntity {

    /** UnitType.constructor 用：返回一个新实例。 */
    public static SegmentUnitEntity create() {
        return new SegmentUnitEntity();
    }

    @Override
    public int classId() {
        return FedUnitEntity.classId(SegmentUnitEntity.class);
    }
}
