package com.kodu16.vsie.content.turret;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

public class TurretProperty {
    @Getter @Setter public volatile Matrix3d coordAxis = new Matrix3d();    //我们规定 模型渲染中 不进行旋转的FACING对应此处单位矩阵

    @Getter @Setter public volatile Vector3d basePivotOffset = new Vector3d();   //枢轴点偏移
    @Getter @Setter public volatile Vector3d worldPivotOffset = new Vector3d();   //枢轴点偏移
    
    public Vector3d transformPivotOffset(ShipTransform transform){
        this.worldPivotOffset =
        transform.getShipToWorld()
                .transformDirection(
                        this.basePivotOffset.normalize().mul(this.basePivotOffset.length()
                        )
                );
        return this.worldPivotOffset;
    }
}
