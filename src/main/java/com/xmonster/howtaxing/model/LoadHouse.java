package com.xmonster.howtaxing.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoadHouse extends House {
    private boolean complete = false;   // 제산세정보 입력까지 완료 여부
}
