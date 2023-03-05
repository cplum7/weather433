<!DOCTYPE html>
<html>
<head>
<title>Remote Temperature</title>
<meta charset="UTF-8">
<link rel="stylesheet" href="pikaday_css/pikaday.css">
<link rel="stylesheet" href="pikaday_css/theme.css">
<link rel="stylesheet" href="pikaday_css/site.css">
<style>
body {
    background-color:powderblue;
}
h1 {
    margin-top: 5px;
    text-align:center;
}
p {
    margin-top: 1px;
}
#temp {
    margin-top: 0px;
    margin-bottom: 1px;
    font-size:40px;
    text-align:center;
    color: blue;
    margin: 0 auto;
    width: 100px;
}
.date {
    font-size:20px;
    text-align:center;
    color: blue;
}
.blk {
    color: black;
}
.blue {
    color: blue;
}
.red {
    color: red;
}
/* Tooltip container */
.tooltip {
  position: relative;
  display: inline-block;
}

/* Tooltip text */
.tooltip .tooltiptext {
  margin-top: 5px;
  text-align:center;
  visibility: hidden;
  width: 70px;
  font-size:10px;
  background-color: black;
  color: #fff;
  padding: 5px 0;
  border-radius: 6px;
 
  /* Position the tooltip text - see examples below! */
  position: absolute;
  z-index: 1;
}

/* Show the tooltip text when you mouse over the tooltip container */
.tooltip:hover .tooltiptext {
  visibility: visible;
}
</style>
</head>
<body>
<div class="tooltip"><span class="tooltiptext">Version 2.10.2</span>&nbsp;</div>
<h1> Current Outdoor Temperature</h1>
<p id="date" class="date"></p>
<p style="float: left"><button id="displayC" onClick="tempDisplay()">Display Centigrade</button></p>
<p id="temp"><b>&degF</b></p>
<div style="margin-top: 1px;">
<p id="selectedDate"></p>
<input type="hidden" id="datepicker">
<button id="datepicker-button">Choose Date</button>
</div>

<table style="float: left;margin-right: 100px;">
    <thead>
        <tr>
            <th id="month"; colspan="10"></th>
        </tr>
        <tr>
            <th>Date</th>
            <th>Time</th>
            <th>High</th>
            <th>Time</th>
            <th>Low</th>
            <th style="padding-left:40px">Date</th>
            <th>Time</th>
            <th>High</th>
            <th>Time</th>
            <th>Low</th>
        </tr>
    </thead>
    <tbody>
        
       <?php 
       for ($i=0; $i < 16; $i++) { ?>
        <tr>
            <td id="<?php echo "idD".$i; ?>" class="blk"></td>
            <td id="<?php echo "idTH".$i; ?>" class="blk"></td>
            <td id="<?php echo "idH".$i; ?>" class="blk"></td>
            <td id="<?php echo "idTL".$i; ?>" class="blk"></td>
            <td id="<?php echo "idL".$i; ?>" class="blk"></td>
            <?php $j=$i + 16;?>
            <td id="<?php echo "idD".$j; ?>" class="blk" style="padding-left: 40px;"></td>
            <td id="<?php echo "idTH".$j; ?>" class="blk"></td>
            <td id="<?php echo "idH".$j; ?>" class="blk"></td>
            <td id="<?php echo "idTL".$j; ?>" class="blk"></td>
            <td id="<?php echo "idL".$j; ?>" class="blk"></td>
        </tr>
    <?php } ?>
    </tbody>
</table>

<table style="float: left;">
    <thead>
        <tr>
            <th></th>
            <th>Day</th>
            <th></th>
            <th>Night</th>
            <th></th>
        </tr>
        <tr>
            <th>Time</th>
            <th>High</th>
            <th>Low</th>
            <th>High</th>
            <th>Low</th>
        </tr>
    </thead>
    <tbody>
       
       <?php $times = array("&nbsp;&nbsp;6:00-7:00&nbsp;&nbsp;", "&nbsp;&nbsp;7:00-8:00&nbsp;&nbsp;", "&nbsp;&nbsp;8:00-9:00&nbsp;&nbsp;", "&nbsp;&nbsp;9:00-10:00&nbsp;", "10:00-11:00&nbsp;&nbsp;&nbsp;", "11:00-12:00&nbsp;&nbsp;&nbsp;", "12:00-1:00&nbsp;", "&nbsp;&nbsp;1:00-2:00&nbsp;&nbsp;", "&nbsp;&nbsp;2:00-3:00&nbsp;&nbsp;", "&nbsp;&nbsp;3:00-4:00&nbsp;&nbsp;", "&nbsp;&nbsp;4:00-5:00&nbsp;&nbsp;", "&nbsp;&nbsp;5:00-6:00&nbsp;&nbsp;");
       for ($i=0; $i <12; $i++) { ?>
        <tr>
            <td><?php echo $times[$i]; ?></td>
            <td id="<?php echo "idDH".$i; ?>" class="blk"></td>
            <td id="<?php echo "idDL".$i; ?>" class="blk"></td>
            <td id="<?php echo "idNH".$i; ?>" class="blk"></td>
            <td id="<?php echo "idNL".$i; ?>" class="blk"></td>
        </tr>
    <?php } ?>
    </tbody>
</table>

<script src="pikaday.js"></script>
    <script>
    var months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    // temps from DB and current temp updates
    var displayedMonthTemps = [];
    
    // max and min monthly temp
    var monthlyHighTemp;
    var monthlyLowTemp;
    
    // array of temps for each day of month table
    var dayHighTemps = [];
    var dayLowTemps = [];
    var dayHighTime = [];
    var dayLowTime = [];
    
    // array of temps for each hr of day table
    var hrHighTemps = [];
    var hrLowTemps = [];
    
    // max and min day temps
    var maxDayTemp = 0;
    var minDayTemp = 9999;
    var maxNightTemp = 0;
    var minNightTemp = 9999;

    var dbStartTime = 0;
    var displayedDate;
    var picker;
    var xmlhttpTimeout = 10000;
    var displayC = false;

    displayDate(new Date());
    getMonthTemps(new Date());
    setDatePicker();
    
    function tempDisplay() {
    	document.getElementById("displayC").innerHTML = (displayC) ? "Display Centigrade" : "Display Fahrenheit";
    	displayC = (displayC) ? false : true;
    	getMonthTemps(new Date());
    }
    
    function updateDisplayTables(curTimeTemp) {
    	picker.setMaxDate(new Date());
    	setCurDisplayTemp(curTimeTemp[0],curTimeTemp[1]);
		timeH = curTimeTemp[0];
		tempH = curTimeTemp[1];
		dbStartTime = curTimeTemp[2];
		timeL = timeH;
		tempL = tempH;
		date = new Date(parseInt(timeH) * 1000);
		dayOfMonth = date.getDate();
		hrOfDay = date.getHours();
		tmpDate = new Date(displayedDate.toString());
		tmpDate.setDate(tmpDate.getDate() + 1);
		nextDisplayedDay = tmpDate.getDate(); // 1-31
		nextDisplayedYear = tmpDate.getFullYear();
		// check if current year is displayed or if next displayed day
		// is current year
		isCurYr = (date.getFullYear() == displayedDate.getFullYear()) ||
				(date.getFullYear() == nextDisplayedYear);				
		tmpDate = new Date(displayedDate.toString());
		tmpDate.setMonth(tmpDate.getMonth() + 1);
		nextDisplayedMonth = tmpDate.getMonth();
		processCurTemp = false;
		// is current month displayed and transitioning to next month
		monthTransition = isCurYr && (nextDisplayedDay == 1) && (date.getMonth() == nextDisplayedMonth) && (hrOfDay >= 6);
		if (monthTransition)
			initDisplayedMonthTemps();
		// if curTimeTemp is for next or current displayed month save in displayedMonthTemps array
		if (isCurYr && (monthTransition || ((date.getMonth() == displayedDate.getMonth()) && (hrOfDay >= 6)) ||
				((date.getMonth() == nextDisplayedMonth) && (hrOfDay < 6)))) {
			hrOfDayIndx = (hrOfDay < 6) ? hrOfDay + 18 : hrOfDay - 6;
			indx = (((dayOfMonth - 1) * 24 * 4) + (hrOfDayIndx)*4);
			if (tempH > displayedMonthTemps[indx+1]) {
				displayedMonthTemps[indx] = timeH;
				displayedMonthTemps[indx+1] = tempH;
			}
			if (tempL < displayedMonthTemps[indx+3]) {
				displayedMonthTemps[indx+2] = timeL;
				displayedMonthTemps[indx+3] = tempL;
			}
			processCurTemp = true;
		}
		// if current day is displayed and transitioning to next day
		// check for monthTransition since if last day e.g. 31 + 1 doesn't equal next day
		if (isCurYr && (monthTransition || (dayOfMonth == displayedDate.getDate() + 1) && (hrOfDay >= 6))) {
			initDayTemps(date);
			displayDate(new Date());
			consoleLog("transitioning to next day");
		}
		// process the current temp if the current or next month is displayed
		if (processCurTemp) {
			processDayTemp(timeH, tempH, timeL, tempL);
			setDayTableRows();
			if (processMonthTemp(timeH, tempH, timeL, tempL))
				setMonthTableRows();
		}
		setTimeout(getCurrentTemp, 40000);
    }
    
    function initDisplayedMonthTemps() {
    	displayedMonthTemps = [];
    	for (i = 0; i < 24 * 31 * 4; i+=4) {
    		displayedMonthTemps[i] = 0;
    		displayedMonthTemps[i+1] = 0;
    		displayedMonthTemps[i+2] = 0;
    		displayedMonthTemps[i+3] = 9999;
    	}
    }
    
    function consoleLog(msg) {
    	curDate = new Date();
    	curDateStr = months[curDate.getMonth()] + " " + curDate.getDate() +
            "," + curDate.getFullYear() + " " + addZero(curDate.getHours()) + ":" +
            addZero(curDate.getMinutes()) + ":" + addZero(curDate.getSeconds());
    	console.log(curDateStr + " " + msg);
    }
    
    function setDayTableRows() {
        for (i=0; i <12; i++) {
            var clr = (hrHighTemps[i] == maxDayTemp && maxDayTemp > 0) ? "red" : "black";
            document.getElementById("idDH"+i).setAttribute("class", clr);
            document.getElementById("idDH"+i).innerHTML = decodeTemp(hrHighTemps[i]);
            clr = (hrLowTemps[i] == minDayTemp && minDayTemp < 9999) ? "blue" : "black";
            document.getElementById("idDL"+i).setAttribute("class", clr);
            document.getElementById("idDL"+i).innerHTML = decodeTemp(hrLowTemps[i]);
            clr = (hrHighTemps[i+12] == maxNightTemp && maxNightTemp > 0) ? "red" : "black";
            document.getElementById("idNH"+i).setAttribute("class", clr);
            document.getElementById("idNH"+i).innerHTML = decodeTemp(hrHighTemps[i+12]);
            clr = (hrLowTemps[i+12] == minNightTemp && minNightTemp < 9999) ? "blue" : "black";
            document.getElementById("idNL"+i).setAttribute("class", clr);
            document.getElementById("idNL"+i).innerHTML = decodeTemp(hrLowTemps[i+12]);
        }
    }
    
    function setMonthTableRows() {
    	inittMonthTable();
    	tmpDate = new Date(displayedDate.toString());
    	tmpDate.setDate(1);
    	tmpDate.setMonth(tmpDate.getMonth() +1);
    	numDays = tmpDate.getDate(tmpDate.setDate(0));
    	rows = Math.floor(numDays/2 + numDays%2);
    	j = 0;
    	for (i = 0; i < numDays; i++) {
    		if (j==rows)
    			j=16;
    		document.getElementById("idD"+j).innerHTML = i + 1;
            document.getElementById("idTH"+j).innerHTML = getFormatTime(dayHighTime[i]);
            clr = (dayHighTemps[i] == monthlyHighTemp) ? "red" : "black";
            document.getElementById("idH"+j).setAttribute("class", clr);
            document.getElementById("idH"+j).innerHTML = decodeTemp(dayHighTemps[i]);
            document.getElementById("idTL"+j).innerHTML = getFormatTime(dayLowTime[i]);
            clr = (dayLowTemps[i] == monthlyLowTemp) ? "blue" : "black";
            document.getElementById("idL"+j).setAttribute("class", clr);
            document.getElementById("idL"+j).innerHTML = decodeTemp(dayLowTemps[i]);
            j++;
    	}   
    }
    
    // blank out rows when long month precedes short month
    function inittMonthTable() {
    	i = 13;
        for (j=0; j< 6; j++) {           
            document.getElementById("idD"+i).innerHTML = "";
            document.getElementById("idTH"+i).innerHTML = "";
            document.getElementById("idH"+i).innerHTML = "";
            document.getElementById("idTL"+i).innerHTML = "";
            document.getElementById("idL"+i).innerHTML = "";
            i++;
            if (j==2)
            	i = 29;
        }
    }
    
    function setCurDisplayTemp(time, temp) {
        curDate = new Date(parseInt(time) * 1000);
        curDateStr = months[curDate.getMonth()] + " " + curDate.getDate() +
            ", " + curDate.getFullYear() + " " + addZero(curDate.getHours()) + ":" +
            addZero(curDate.getMinutes());
        document.getElementById("date").innerHTML = curDateStr;
        document.getElementById("temp").innerHTML = "<b>" + decodeTemp(temp) + "&degF</b>";
    }
    
    function getCurrentTemp() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.ontimeout = function () {
			consoleLog("XMLHttpRequest timeout");
			setTimeout(getCurrentTemp, 40000);
		};
		xmlhttp.onerror = function () {
			consoleLog("XMLHttpRequest error");
			setTimeout(getCurrentTemp, 40000);
		};
        xmlhttp.onreadystatechange = function() {
            if (this.readyState == 4 && this.status == 200) {
                curTimeTemp = JSON.parse(this.responseText);
                updateDisplayTables(curTimeTemp);
            }
        };
        xmlhttp.open("GET", "getCurrentTimeTemp.php", true);
        xmlhttp.timeout = xmlhttpTimeout;
        xmlhttp.send();
    }
        
    function getMonthTemps(date) {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function() {
        	if (this.readyState == 4 && this.status == 200) {
                reponse = JSON.parse(this.responseText);
                initDisplayedMonthTemps();
                for (i=0;i < reponse.length; i++)
                	displayedMonthTemps[i] = reponse[i];
                initMonthTemps();
                setMonthTableRows();
                initDayTemps(date);
            	setDayTableRows();
            	getCurrentTemp();
            }
        };
        monthVals = getBeginEndMonth(date);
        beginMonth = monthVals[0];
        endMonth = monthVals[1];
        xmlhttp.open("GET", "getTemps.php/?time1=" + beginMonth + "&time2=" + endMonth);
        xmlhttp.send();
    }
       
    function initMonthTemps() {
    	monthlyHighTemp = 0;
    	monthlyLowTemp = 9999;
    	for (i=0; i < 31; i++) {
    		dayHighTime[i] = 0;
    		dayHighTemps[i] = 0;
    		dayLowTime[i] = 0;
    		dayLowTemps[i] = 9999;
    	}
  	
    	for (i = 0; i < displayedMonthTemps.length; i+=4) {
    		timeH = displayedMonthTemps[i];
	    	tempH = displayedMonthTemps[i+1];
	    	timeL = displayedMonthTemps[i+2];
	    	tempL = displayedMonthTemps[i+3];
    		processMonthTemp(timeH, tempH, timeL, tempL);
    	}
    }
    
    function processMonthTemp(timeH, tempH, timeL, tempL) {
    	updated = false;
    	date = new Date(parseInt(timeH) * 1000);
		if (date.getHours() < 6)
			date.setDate(date.getDate() - 1);
		indx = date.getDate() - 1;
		if (tempH > dayHighTemps[indx]) {
			dayHighTemps[indx] = tempH;
			dayHighTime[indx] = timeH;
			updated = true;
		}
		if (tempL < dayLowTemps[indx]) {
			dayLowTemps[indx] = tempL;
			dayLowTime[indx] = timeL;
			updated = true;
		}		
		if (tempH > monthlyHighTemp) {
			monthlyHighTemp = tempH;
			updated = true;
		}
		
		if (tempL < monthlyLowTemp) {
			monthlyLowTemp = tempL;
			updated = true;
		}		
		return updated;
    }
    
    function initDayTemps(date) {
    	maxDayTemp = 0;
		minDayTemp = 9999;
		maxNightTemp = 0;
		minNightTemp = 9999;
    	for (i=0; i < 24; i++) {
    		hrHighTemps[i] = 0;
    		hrLowTemps[i] = 9999;
    	}
    	
    	dayVals = getBeginEndDay(date);
    	beginDay = dayVals[0];
        endDay = dayVals[1];
    	for (i = 0; i < displayedMonthTemps.length; i+=4) {
    		timeH = displayedMonthTemps[i];
	    	tempH = displayedMonthTemps[i+1];
	    	timeL = displayedMonthTemps[i+2];
	    	tempL = displayedMonthTemps[i+3];
    		if (timeH >= beginDay  && timeH < endDay)
    			processDayTemp(timeH, tempH, timeL, tempL, false);
    	}
    }
    
    function processDayTemp(timeH, tempH, timeL, tempL) {
    	updated = false;
    	date = new Date(parseInt(timeH) * 1000);
		dayOfMonth = date.getDate();
		hrOfDay = date.getHours();
		hrOfDayIndx = (hrOfDay < 6) ? hrOfDay + 18 : hrOfDay - 6;
		hrHighTemps[hrOfDayIndx] = Math.max(tempH, hrHighTemps[hrOfDayIndx]);
		hrLowTemps[hrOfDayIndx] = Math.min(tempL, hrLowTemps[hrOfDayIndx]);
		if (hrOfDay >= 6 && hrOfDay < 18) {
			maxDayTemp = Math.max(tempH, maxDayTemp);
			minDayTemp = Math.min(tempL, minDayTemp);
		} else {
			maxNightTemp = Math.max(tempH, maxNightTemp);
			minNightTemp = Math.min(tempL, minNightTemp);
		}
    }
    
    function setDatePicker() {
        var field = document.getElementById('datepicker');
        picker = new Pikaday(
        {        
          field: document.getElementById('datepicker'),
            trigger: document.getElementById('datepicker-button'),
            theme: 'dark-theme',
            minDate: new Date(parseInt(dbStartTime) * 1000),
            onSelect: function(date) {
            	date.setHours(6);
            	 changedMonth = (date.getMonth() != displayedDate.getMonth()) ||
            	 	(date.getFullYear() != displayedDate.getFullYear());  
            	 displayDate(date);
            	 if (changedMonth)
            		 getMonthTemps(date);
            	 else {
            		 initDayTemps(date);
            		 setDayTableRows();
            	 }
            }
        });
    }
    
    function getBeginEndDay(date) {
    	return getBeginEndPeriod(date, "day");
    }
    
    function getBeginEndMonth(date) {
    	return getBeginEndPeriod(date, "month");
    }
    
    function getBeginEndPeriod(date, period) {
    	periodVals = [];
    	tmpDate = new Date(date.toString());
    	if ((tmpDate.getHours()) < 6)
    		tmpDate.setDate(tmpDate.getDate()-1);
    	tmpDate.setHours(6);
    	tmpDate.setMinutes(0);
    	tmpDate.setSeconds(0);
    	tmpDate.setMilliseconds(0);
		dayBegin = tmpDate.getTime()/(1000);	
		tmpDate.setDate(tmpDate.getDate() + 1);
		dayEnd = tmpDate.getTime()/(1000);
		tmpDate.setDate(1);
        monthBegin = tmpDate.getTime()/(1000);
        tmpDate.setMonth(tmpDate.getMonth() + 1);
        monthEnd = tmpDate.getTime()/(1000);
        if (period == "month") {
        	periodVals[0] = monthBegin;
        	periodVals[1] = monthEnd;
        } 
        if (period == "day") {
        	periodVals[0] = dayBegin;
        	periodVals[1] = dayEnd;
        }
		return periodVals;
    }
    
    function addZero(i) {
        if (i < 10) {
            i = "0" + i;
        }
        return i;
    }
    
    function getFormatTime(inTime) {
        var rtnVal = "-";
        if ((inTime > 0) && (typeof inTime !== 'undefined')) {
            date = new Date(parseInt(inTime) * 1000);
            rtnVal = addZero(date.getHours()) + ":" + addZero(date.getMinutes());
        }
        return rtnVal;
    }
    
    function displayDate(date) {
    	if ((date.getHours()) < 6)
        	date.setDate(date.getDate()-1);
    	date.setHours(6);
		date.setMinutes(0);
		date.setSeconds(0);
		date.setMilliseconds(0);
    	monthName = months[date.getMonth()];
    	document.getElementById('selectedDate').innerHTML = monthName + " " + date.getDate() + ", " + date.getFullYear();
		document.getElementById("month").innerHTML = monthName;
		displayedDate = date;
    }
    
    function decodeTemp(inTemp) {
        var x2 = "-";
        // temp range -40C to 70C
        if (inTemp > 0 && inTemp < 1100) {
            var x1 = inTemp - 400;
            if (displayC)
            	x2 = x1/10;
            else
            	x2 = (x1 * 18 + 3200)/100;
            	
            x2 = x2.toFixed(1);
        }
        return x2;
    }
    </script>
</body>
</html>