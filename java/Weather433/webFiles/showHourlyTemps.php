<?php
// http://URL/showHourlyTemps.php/?time1=46918830&time2=49302281&convert=Y
// http://URL/showHourlyTemps.php?version

$version = "2.9";
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

	$time1 = $_GET["time1"];
	$time2 = $_GET["time2"];
	$convert = $_GET["convert"];

	//$sql = "SELECT * FROM ".$dbname.".`HourlyTemps` where maxTime >= ".$time1." and ".$rows;
	$sql = "SELECT * FROM HourlyTemps where maxTime >= ".$time1." and maxTime < ".$time2;
	date_default_timezone_set("America/Los_Angeles");

	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
		$stmt->setFetchMode(PDO::FETCH_NUM); 
		$result = $stmt->fetchAll();
		foreach ($result as $key => $val) {   	
			$unixTime1 = $val[0];
			$timeH = strftime("%D %T", $unixTime1);
			$unixTime2 = $val[2];
			$timeL = strftime("%D %T", $unixTime2);
			$xH1 = (($val[1] - 400) * 18 + 3200)/100;
			$xH2 = number_format($xH1,1,'.',"");
			$xL1 = (($val[3] - 400) * 18 + 3200)/100;
			$xL2 = number_format($xL1,1,'.',"");
			if ($convert == "Y")
				//echo nl2br($timeH." ".$val[0]." ".$val[1]." ".$timeL." ".$val[2]." ".$val[3].PHP_EOL);
				echo nl2br($timeH."=".$val[0]."&nbsp &nbsp".$xH2."=".$val[1]."&nbsp &nbsp &nbsp &nbsp".$timeL."=".$val[2]."&nbsp &nbsp".$xL2."=".$val[3].PHP_EOL);
			else
				echo nl2br($val[0].",".$val[1].",".$val[2].",".$val[3].",".PHP_EOL);
		}
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
	$conn = null;
}
?>