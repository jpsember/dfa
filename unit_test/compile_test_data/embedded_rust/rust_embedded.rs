    let target_file = file_absolute(&PathBuf::from("error_summary.json"))?;
    file_assert_is_dir(&target_file.parent().unwrap(), "error summary destination")?;
    file_write_string(&target_file, &jslist.print_compact())?;
    Ok(())
}

/*
    Embedding DFA into source code experiment

    =DFA:SOURCE_CODE:rxp        <<<< marks start of 'rxp' source

    WS:  $_WHITESPACE

    COMMENT:  $_JAVACOMMENT | $_HASHCOMMENT

    PUB: pub
    CONST: const
    ERRORDECL: ERROR_ $_ID
    ERROR2: \: \s* usize \s* \=
    INTEGER: $_UNSIGNEDINT

    =DFA                        <<<< marks end of 'rxp' source section

*/

/* =DFA:SOURCE_CODE:tokens */
const T_WS: i32 = 0;
const T_COMMENT: i32 = 1;
const T_PUB: i32 = 2;
const T_CONST: i32 = 3;
const T_ERRORDECL: i32 = 4;
const T_ERROR2: i32 = 5;
const T_INTEGER: i32 = 6;
/* =DFA                          <<< marks end of token defs */

/* =DFA:SOURCE_CODE:dfa */
const dfa_str_src_code: &str = r#"{"graph":[0,9,1,112,1,18,1,1,99,1,-12,0,1,69,1,-71,0,1,58,1,-125,0,1,49,9,124,0,1,48,1,122,0,1,47,1,73,0,1,35,1,62,0,3,9,2,12,2,32,1,51,0,1,1,3,9,2,12,2,32,1,51,0,2,1,3,1,9,11,2,14,114,62,0,0,2,1,42,1,85,0,1,47,1,62,0,0,2,2,1,41,43,85,85,0,1,42,1,99,0,0,3,3,1,41,43,4,48,80,85,0,1,42,1,99,0,1,47,1,120,0,2,0,7,0,7,1,1,48,10,124,0,0,2,1,32,1,-125,0,1,117,1,-113,0,0,1,1,115,1,-106,0,0,1,1,105,1,-99,0,0,1,1,122,1,-92,0,0,1,1,101,1,-85,0,0,2,1,32,1,-85,0,1,61,1,-73,0,6,0,0,1,1,82,1,-64,0,0,1,1,82,1,-57,0,0,1,1,79,1,-50,0,0,1,1,82,1,-43,0,0,1,1,95,1,-36,0,0,1,3,65,26,95,1,97,26,-25,0,5,1,4,48,10,65,26,95,1,97,26,-25,0,0,1,1,111,1,-5,0,0,1,1,110,1,2,1,0,1,1,115,1,9,1,0,1,1,116,1,16,1,4,0,0,1,1,117,1,25,1,0,1,1,98,1,32,1,3,0],"token_names":"WS COMMENT PUB CONST ERRORDECL ERROR2 INTEGER","version":"$2"}"#;
/* =DFA */

static SHARED_ERROR_INFO: LazyLock<Arc<RwLock<ErrorMap>>> =
    LazyLock::new(|| Arc::new(RwLock::new(HashMap::new())));
