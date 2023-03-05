<?php
// http://URL/setStartTimeTemp.php
// http://URL/setStartTimeTemp.php?version

$version = "2.9";
$dbMinTime = 0;
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

	$sql = "SELECT * from HourlyTemps order by maxTime ASC limit 1";

	try {
		$conn = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
		$conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$stmt = $conn->prepare($sql); 
		$stmt->execute();
		$stmt->setFetchMode(PDO::FETCH_NUM); 
		$result = $stmt->fetchAll();
		foreach ($result as $key => $val) {
			$dbMinTime=$val[0];
		}
	} catch(PDOException $e) {
		 echo nl2br("Error: " . $e->getMessage().PHP_EOL);
	}
	$conn = null;
	
	$fileName = "curTimeTemp.txt";
	$timeParam = 0;
	$tempParam = 0;
	foreach(file($fileName) as $line) {
		$tmpArray = explode('=', rtrim($line));
		if ($tmpArray[0] == "time")
			$timeParam = $tmpArray[1];
		if ($tmpArray[0] == "temp") 
			$tempParam = $tmpArray[1];
	}
	
	$myfile = fopen($fileName, "w") or die("Unable to open file!");
	fwrite($myfile, "time=".$timeParam."\n");
	fwrite($myfile, "temp=".$tempParam."\n");
	fwrite($myfile, "minDBTime=".$dbMinTime."\n");
	fclose($myfile);
}
?>