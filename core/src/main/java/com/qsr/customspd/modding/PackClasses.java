package com.qsr.customspd.modding;

import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

public class PackClasses {

	//a pack installed before a class was renamed still names it the old way, as old saves do
	public static Class forName( String name ){
		return Reflection.forName( Bundle.resolveAlias( name ) );
	}

}
