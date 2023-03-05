<?php
// http://URL/getCurrentTimeTemp.php
// http://URL/getCurrentTimeTemp.php?version
// get minimum time in DB for date pickers

$version = "2.9";
if (isset($_GET["version"])) {
	echo nl2br("version=".$version.PHP_EOL);
} else {
	$fileName = "curTimeTemp.txt";
	$valArray = array();
	foreach(file($fileName) as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "time")
			$valArray[0] = $tmpArray[1];
		if ($tmpArray[0] == "temp") 
			$valArray[1] = $tmpArray[1];
		if ($tmpArray[0] == "minDBTime") 
			$valArray[2] = $tmpArray[1];
	}
	$myJSON = json_encode($valArray);
	echo $myJSON;
}
?>