/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.server.worker.task;

import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.utils.FileUtils;
import org.apache.dolphinscheduler.common.utils.StringUtils;
import org.apache.dolphinscheduler.server.entity.TaskExecutionContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * python command executor
 */
public class PythonCommandExecutor extends AbstractCommandExecutor {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PythonCommandExecutor.class);

    /**
     * python
     */
    public static final String PYTHON = "python";
    private static final Pattern PYTHON_PATH_PATTERN = Pattern.compile("/bin/python[\\d.]*$");

    /**
     * constructor
     * @param logHandler    log handler
     * @param taskExecutionContext       taskExecutionContext
     * @param logger        logger
     */
    public PythonCommandExecutor(Consumer<List<String>> logHandler,
                                 TaskExecutionContext taskExecutionContext,
                                 Logger logger) {
        super(logHandler,taskExecutionContext,logger);
    }


    /**
     * build command file path
     *
     * @return command file path
     */
    @Override
    protected String buildCommandFilePath() {
        return String.format("%s/py_%s.command", taskExecutionContext.getExecutePath(), taskExecutionContext.getTaskAppId());
    }

    /**
     * create command file if not exists
     * @param execCommand   exec command
     * @param commandFile   command file
     * @throws IOException  io exception
     */
    @Override
    protected void createCommandFileIfNotExists(String execCommand, String commandFile) throws IOException {
        logger.info("tenantCode :{}, task dir:{}", taskExecutionContext.getTenantCode(), taskExecutionContext.getExecutePath());

        if (!Files.exists(Paths.get(commandFile))) {
            logger.info("generate command file:{}", commandFile);

            StringBuilder sb = new StringBuilder();
            sb.append("#-*- encoding=utf8 -*-\n");

            sb.append("\n\n");
            sb.append(execCommand);
            logger.info(sb.toString());

            // write data to file
            FileUtils.writeStringToFile(new File(commandFile),
                    sb.toString(),
                    StandardCharsets.UTF_8);
        }
    }

    /**
     * get command options
     * @return command options list
     */
    @Override
    protected List<String> commandOptions() {
        // unbuffered binary stdout and stderr
        return Collections.singletonList("-u");
    }

    /**
     * Gets the command path to which Python can execute
     * @return python command path
     */
    @Override
    protected String commandInterpreter() {
        String pythonHome = getPythonHome(taskExecutionContext.getEnvFile());
        return getPythonCommand(pythonHome);
    }

    /**
     * get python command
     *
     * @param pythonHome python home
     * @return python command
     */
    public static String getPythonCommand(String pythonHome) {
        if (StringUtils.isEmpty(pythonHome)) {
            return PYTHON;
        }
        File file = new File(pythonHome);
        if (file.exists() && file.isFile()) {
            return pythonHome;
        }
        if (PYTHON_PATH_PATTERN.matcher(pythonHome).find()) {
            return pythonHome;
        }
        return pythonHome + "/bin/python";
    }

    /**
     * get python home
     *
     * @param envPath env path
     * @return python home
     */
    public static String getPythonHome(String envPath) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(envPath)));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(Constants.PYTHON_HOME)) {
                    sb.append(line);
                    break;
                }
            }
            String result = sb.toString();
            if (StringUtils.isEmpty(result)) {
                return null;
            }
            String[] arrs = result.split(Constants.EQUAL_SIGN);
            if (arrs.length == 2) {
                return arrs[1];
            }
        } catch (IOException e) {
            logger.error("read file failure", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

}
