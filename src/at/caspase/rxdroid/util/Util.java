/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 * 
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package at.caspase.rxdroid.util;

import at.caspase.rxdroid.Database;
import at.caspase.rxdroid.R;

public class Util
{
	public static int getDoseTimeDrawableFromDoseViewId(int doseViewId)
    {
        switch(doseViewId)
        {
                case R.id.morning:
                        return R.drawable.ic_morning;
                case R.id.noon:
                        return R.drawable.ic_noon;
                case R.id.evening:
                        return R.drawable.ic_evening;
                case R.id.night:
                        return R.drawable.ic_night;
        }
        
        throw new IllegalArgumentException();
    }
    
    public static int getDoseTimeFromDoseViewId(int doseViewId)
    {
        switch(doseViewId)
        {
                case R.id.morning:
                        return Database.Drug.TIME_MORNING;
                case R.id.noon:
                        return Database.Drug.TIME_NOON;
                case R.id.evening:
                        return Database.Drug.TIME_EVENING;
                case R.id.night:
                        return Database.Drug.TIME_NIGHT;
        }
        
        throw new IllegalArgumentException();           
    }
}
