package com.example.data.repository

import com.example.data.models.Exercise

object ExerciseData {
    val allExercises = listOf(
        // Strength Training
        Exercise(
            id = 1,
            name = "Squat",
            category = "STRENGTH",
            musclesWorked = listOf("Quads", "Glutes", "Hamstrings", "Core"),
            steps = listOf(
                "Stand with feet shoulder-width apart, chest up, and hands in front.",
                "Lower your hips as if sitting back in a chair.",
                "Keep knees behind toes, thighs parallel to the floor.",
                "Drive through your heels to return to starting position."
            )
        ),
        Exercise(
            id = 2,
            name = "Lunge",
            category = "STRENGTH",
            musclesWorked = listOf("Quads", "Glutes", "Hamstrings"),
            steps = listOf(
                "Step forward with one leg, lowering hips until both knees are bent at 90 degrees.",
                "Keep rear knee hovering just above the floor.",
                "Push back up to the starting position.",
                "Repeat on the opposite leg."
            )
        ),
        Exercise(
            id = 3,
            name = "Deadlift",
            category = "STRENGTH",
            musclesWorked = listOf("Hamstrings", "Glutes", "Lower Back", "Traps"),
            steps = listOf(
                "Stand with feet mid-foot under a barbell or dumbbells.",
                "Bend over and grab the weight with a flat back.",
                "Drive hips forward to lift the weight, keeping it close to your shins.",
                "Stand tall, locking out hips and shoulders.",
                "Lower back down with control, keeping back completely straight."
            )
        ),
        Exercise(
            id = 4,
            name = "Bench Press",
            category = "STRENGTH",
            musclesWorked = listOf("Chest", "Triceps", "Front Deltoids"),
            steps = listOf(
                "Lie flat on a bench, gripping the barbell slightly wider than shoulder-width.",
                "Unrack the bar and lower it slowly to mid-chest level.",
                "Press the bar upward explosively until arms are locked out.",
                "Maintain a tight back and flat foot base throughout."
            )
        ),
        Exercise(
            id = 5,
            name = "Overhead Press",
            category = "STRENGTH",
            musclesWorked = listOf("Shoulders", "Triceps", "Core"),
            steps = listOf(
                "Set the bar or dumbbells at shoulder height.",
                "Press the weight directly overhead, locking out your arms.",
                "Shrug shoulders upward at the top of the lift.",
                "Lower the weight back to the collarbone with control."
            )
        ),
        Exercise(
            id = 6,
            name = "Pull-up",
            category = "STRENGTH",
            musclesWorked = listOf("Lats", "Rhomboids", "Biceps", "Forearms"),
            steps = listOf(
                "Hang from a bar with palms facing away, shoulder-width apart.",
                "Pull chest to the bar by squeezing shoulder blades together and driving elbows down.",
                "Keep core tight and avoid kicking or swinging.",
                "Lower with control until arms are fully extended."
            )
        ),
        Exercise(
            id = 7,
            name = "Push-up",
            category = "STRENGTH",
            musclesWorked = listOf("Chest", "Triceps", "Front Deltoids", "Core"),
            steps = listOf(
                "Place hands shoulder-width apart on the floor, toes tucked.",
                "Maintain a rigid straight line from head to heels.",
                "Lower chest to the floor by bending elbows at a 45-degree angle.",
                "Push back up to the starting position."
            )
        ),
        Exercise(
            id = 8,
            name = "Barbell Row",
            category = "STRENGTH",
            musclesWorked = listOf("Upper Back", "Lats", "Rear Delts", "Biceps"),
            steps = listOf(
                "Hinge at the waist with a flat back, holding the bar at arm's length.",
                "Pull the bar to your lower ribcage, leading with the elbows.",
                "Squeeze shoulder blades tightly at the peak.",
                "Lower the weight back down with full control."
            )
        ),
        Exercise(
            id = 9,
            name = "Bicep Curl",
            category = "STRENGTH",
            musclesWorked = listOf("Biceps", "Forearms"),
            steps = listOf(
                "Stand tall holding dumbbells at your sides, palms facing forward.",
                "Squeeze your biceps to curl the weights up toward shoulders.",
                "Keep elbows pinned to your sides; do not swing hips.",
                "Lower slowly back to the starting position."
            )
        ),
        Exercise(
            id = 10,
            name = "Tricep Extension",
            category = "STRENGTH",
            musclesWorked = listOf("Triceps"),
            steps = listOf(
                "Hold a dumbbell overhead with both hands.",
                "Lower the weight behind your head by bending at the elbows.",
                "Keep your upper arms stationary and close to your ears.",
                "Extend back upward to complete the rep."
            )
        ),
        Exercise(
            id = 11,
            name = "Calf Raise",
            category = "STRENGTH",
            musclesWorked = listOf("Calves"),
            steps = listOf(
                "Stand adjacent to a wall or support, feet hip-width.",
                "Raise your heels as high as possible, standing on the balls of your feet.",
                "Squeeze calves at the peak.",
                "Lower heels back to the floor slowly."
            )
        ),
        Exercise(
            id = 12,
            name = "Glute Bridge",
            category = "STRENGTH",
            musclesWorked = listOf("Glutes", "Hamstrings", "Lower Back"),
            steps = listOf(
                "Lie on your back with knees bent and feet flat on the floor.",
                "Drive through heels to lift hips, forming a straight line from knees to shoulders.",
                "Squeeze glutes tightly at the top.",
                "Lower hips down to touch the floor gently."
            )
        ),

        // Core
        Exercise(
            id = 13,
            name = "Plank",
            category = "CORE",
            musclesWorked = listOf("Abs", "Obliques", "Shoulders", "Glutes"),
            steps = listOf(
                "Place forearms on the floor, elbows aligned under shoulders.",
                "Step feet back so your body forms a straight line from head to toe.",
                "Brace your core, squeeze glutes, and draw shoulders back.",
                "Hold this rigid position, breathing deeply."
            )
        ),
        Exercise(
            id = 14,
            name = "Crunch",
            category = "CORE",
            musclesWorked = listOf("Upper Abs"),
            steps = listOf(
                "Lie on your back, knees bent, feet flat, hands behind head.",
                "Contract abs to lift shoulders 2-3 inches off the ground.",
                "Keep lower back pressed flat into the floor.",
                "Exhale on the way up, inhale as you lower slowly."
            )
        ),
        Exercise(
            id = 15,
            name = "Sit-up",
            category = "CORE",
            musclesWorked = listOf("Abs", "Hip Flexors"),
            steps = listOf(
                "Lie on your back, knees bent, ankles anchored if needed.",
                "Cross arms on chest or place fingers lightly behind ears.",
                "Lift your torso all the way up to a sitting position.",
                "Lower back down with control step-by-step."
            )
        ),
        Exercise(
            id = 16,
            name = "Russian Twist",
            category = "CORE",
            musclesWorked = listOf("Obliques", "Abs"),
            steps = listOf(
                "Sit on the floor with knees bent and feet slightly elevated.",
                "Lean torso back at a 45-degree angle, keeping back straight.",
                "Clasp hands and twist torso from side to side, touching the floor.",
                "Perform smoothly without twisting the lower spine."
            )
        ),
        Exercise(
            id = 17,
            name = "Mountain Climber",
            category = "CORE",
            musclesWorked = listOf("Abs", "Shoulders", "Cardio Engine"),
            steps = listOf(
                "Start in a high plank position.",
                "Drive knee toward your chest as fast as possible.",
                "Switch legs rapidly, keeping hips level and low.",
                "Repeat in a running rhythm."
            )
        ),

        // Cardio
        Exercise(
            id = 18,
            name = "Running",
            category = "CARDIO",
            musclesWorked = listOf("Quads", "Cardio Engine", "Calves", "Glutes"),
            steps = listOf(
                "Maintain a comfortable posture, gaze forward.",
                "Land soft on the mid-foot, driving heels back.",
                "Keep shoulders relaxed with steady rhythmic breathing.",
                "Adjust pace based on your stamina block."
            )
        ),
        Exercise(
            id = 19,
            name = "Cycling",
            category = "CARDIO",
            musclesWorked = listOf("Quads", "Hamstrings", "Calves", "Cardio Engine"),
            steps = listOf(
                "Sit tall on the saddle, adjust height for a slight knee bend.",
                "Pedal with the balls of your feet in smooth, circular sweeps.",
                "Keep core engaged, breathing steadily.",
                "Vary resistance to simulate trail climbing."
            )
        ),
        Exercise(
            id = 20,
            name = "Swimming",
            category = "CARDIO",
            musclesWorked = listOf("Full Body", "Shoulders", "Lats", "Cardio Engine"),
            steps = listOf(
                "Enter pool, maintain a horizontal glide pattern.",
                "Execute steady stroking motions alternating arms.",
                "Flutter kick smoothly from the hips.",
                "Turn head side-to-side to breathe dynamically."
            )
        ),
        Exercise(
            id = 21,
            name = "Jumping Rope",
            category = "CARDIO",
            musclesWorked = listOf("Calves", "Cardio Engine", "Shoulders"),
            steps = listOf(
                "Hold rope ends at hip height, elbows pinned near ribcage.",
                "Flick wrists to swing the rope over head.",
                "Perform minor jumps, landing strictly on balls of feet.",
                "Maintain a bounding, steady cycle."
            )
        ),
        Exercise(
            id = 22,
            name = "Rowing",
            category = "CARDIO",
            musclesWorked = listOf("Lats", "Legs", "Rhomboids", "Core", "Cardio Engine"),
            steps = listOf(
                "Sit on sliding seat, slide forward bending knees to catch position.",
                "Drive backwards with your legs forcefully.",
                "Leaning slightly back, pull handles to lower chest.",
                "Extend arms and slide forward to reset."
            )
        ),
        Exercise(
            id = 23,
            name = "Burpee",
            category = "CARDIO",
            musclesWorked = listOf("Full Body", "Chest", "Quads", "Triceps", "Cardio Engine"),
            steps = listOf(
                "Stand tall, drop into a deep squat placing hands on floor.",
                "Jump feet back into a full push-up position, lowering chest.",
                "Push back up, snapping feet back under chest.",
                "Explode upward in a straight jump, clapping hands overhead."
            )
        ),

        // Flexibility
        Exercise(
            id = 24,
            name = "Yoga",
            category = "FLEXIBILITY",
            musclesWorked = listOf("Full Body", "Flexibility Core", "Mental Focus"),
            steps = listOf(
                "Roll out mat, start in child's pose to settle breath.",
                "Cycle through down-dog, planks, and cobra flows.",
                "Focus on opening hamstrings and shoulders cleanly.",
                "Hold postures for 5 long deep centering breaths."
            )
        ),
        Exercise(
            id = 25,
            name = "Pilates",
            category = "FLEXIBILITY",
            musclesWorked = listOf("Deep Abs", "Glutes", "Posture Alignment"),
            steps = listOf(
                "Lie on mat, press spine flat to execute pelvic tilts.",
                "Perform double leg stretches, pumping arms up and down.",
                "Control movements completely via slow, deep exhalations.",
                "Target deep stabilizer muscles along the torso."
            )
        ),
        Exercise(
            id = 26,
            name = "Tai Chi",
            category = "FLEXIBILITY",
            musclesWorked = listOf("Balance", "Joint Health", "Calves"),
            steps = listOf(
                "Stand in a comfortable soft-knee athletic pose.",
                "Perform slow, continuous fluid rotational sweeps with arms.",
                "Shift bodyweight deliberately from heel to heel.",
                "Coordinate breaths to expand and retract movements."
            )
        )
    )

    fun getExercisesForDay(dayName: String): List<Exercise> {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val index = days.indexOfFirst { it.equals(dayName, ignoreCase = true) }.coerceAtLeast(0)

        // Return a customized set of 5 exercises for the day of the week
        return when (index) {
            0 -> listOf(
                allExercises[0],  // Squat
                allExercises[3],  // Bench Press
                allExercises[6],  // Push-up
                allExercises[8],  // Bicep Curl
                allExercises[10]  // Calf Raise
            ) // Strength (Upper body bias)
            1 -> listOf(
                allExercises[17], // Running
                allExercises[20], // Jumping Rope
                allExercises[22]  // Burpee
            ) // Cardio
            2 -> listOf(
                allExercises[1],  // Lunge
                allExercises[2],  // Deadlift
                allExercises[5],  // Pull-up
                allExercises[11]  // Glute Bridge
            ) // Strength (Lower body bias)
            3 -> listOf(
                allExercises[12], // Plank
                allExercises[13], // Crunch
                allExercises[14], // Sit-up
                allExercises[15]  // Russian Twist
            ) // Core
            4 -> listOf(
                allExercises[0],  // Squat
                allExercises[4],  // Overhead Press
                allExercises[7],  // Barbell Row
                allExercises[9],  // Tricep Extension
                allExercises[11]  // Glute Bridge
            ) // Strength (Full body)
            5 -> listOf(
                allExercises[18], // Cycling
                allExercises[23], // Yoga
                allExercises[24]  // Pilates
            ) // Cardio + Flexibility
            else -> listOf(
                allExercises[23], // Yoga
                allExercises[25]  // Tai Chi
            ) // Rest & Mindful Flexibility
        }
    }

    fun getExerciseExpValue(exerciseName: String): Int {
        return when (exerciseName.trim().lowercase(java.util.Locale.ROOT)) {
            "deadlift" -> 45
            "pull-up", "pull up" -> 40
            "overhead press" -> 35
            "bench press" -> 35
            "barbell row" -> 35
            "squat" -> 30
            "lunge" -> 25
            "glute bridge" -> 20
            "bicep curl" -> 20
            "tricep extension" -> 20
            "push-up", "push up" -> 20
            "calf raise" -> 15
            // Core
            "mountain climber" -> 25
            "russian twist" -> 20
            "plank", "plank (30s)", "plank (30s hold)" -> 20
            "sit-up", "sit up" -> 15
            "crunch" -> 15
            // Cardio
            "burpee" -> 35
            "rowing" -> 30
            "jumping rope", "jump rope" -> 25
            "running" -> 30
            "cycling" -> 20
            "swimming" -> 25
            // Flexibility
            "tai chi" -> 40
            "pilates" -> 35
            "yoga" -> 30
            else -> 50
        }
    }

    fun getExerciseExpText(exerciseName: String): String {
        val value = getExerciseExpValue(exerciseName)
        val unit = when (exerciseName.trim().lowercase(java.util.Locale.ROOT)) {
            "running", "cycling" -> "km"
            "swimming" -> "100m"
            "tai chi", "pilates", "yoga" -> "session"
            else -> "set"
        }
        return "+$value EXP / $unit"
    }
}
