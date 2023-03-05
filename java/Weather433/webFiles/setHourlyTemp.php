<?php
// http://URL/setHourlyTemp.php/?lastHourlyTime=10479885&maxTime=10480514&maxTemp=553&minTime=10479861&minTemp=542
// http://URL/setHourlyTemp.php/?version

$version = "2.8";
if (isset($_GET["version"])) {
	echo nl2br("version=".$version.PHP_EOL);
} else {
	foreach(file("hostDB.txt") as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "servername")
			$servername = $tmpArray[1];
		if ($tmpArray[0] == "username") 
			$username = $tmpArray[1];
		if ($tmpArray[0] == "password")
			$password = $tmpArray[1];
		if ($tmpArray[0] == "dbname") 
			$dbname = $tmpArray[1];
	}

	$lastHourlyTime = $_GET["lastHourlyTime"];
	$maxTimeParam = $_GET["maxTime"];
	$maxTempParam = $_GET["maxTemp"];
	$minTimeParam = $_GET["minTime"];
	$minTempParam = $_GET["minTemp"];

	if ($lastHourlyTime > 0) {
		$sql = "UPDATE HourlyTemps SET maxTime = ".$maxTimeParam.
		", maxTemp = ".$maxTempParam.", minTime = ".$minTimeParam.
		", minTemp = ".$minTempParam." where maxTime = ".$lastHourlyTime;
	} else {
		$sql = "INSERT INTO HourlyTemps (maxTime, maxTemp, minTime, minTemp)
		VALUES (".$maxTimeParam.",".$maxTempParam.",".$minTimeParam. ",".$minTempParam.")";
	}

	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
	$conn = null;
}
?>