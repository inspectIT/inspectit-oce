import React from 'react'
import { Button } from 'primereact/button'

/* 
* Basic component to display a setting element with Header/Content/Button
* Header Properties (not required) => title: title/heading of the element to display (string) ; fullHeader: header component (React Component)
* Button Properties (not required) => btn: trigger to display button in the lower right corner (true) ; btnLabel: label on button (string) ; 
*   btnIcon: pi icon name (string) ; btnOnClick: onClick Funktion (function)
* Misc (not required) => line: trigger to display a line after element
*/
const SettingsElement = (props) => {
  return (
    <div className={`this ${props.line ? 'line': ''}`}>
      <style jsx>{`
        .this{
          margin: 1rem;
        }
        .header{
          margin: 0;
          margin-bottom: 1rem;
          background: #ddd;
          padding-left: 0.5rem;
          height: 2rem;

        }
        .title{
          font-size: 1rem;
          font-weight: normal;
          position: relative;
          top: 0.25rem;
        }
        .content{
          padding-left: 1rem;
          padding-right: 1rem;
        }
        .settingsEbtn{
          text-align: right;
          margin-bottom: 1rem;
        }
        .line:after{
          content: "";
          display: block;
          border-radius: 25px;
          height: 0.1rem;
          background: #ddd;
          position: relative;
          width: 95%;
          left: calc(5% / 2)
      `}</style>
      {props.fullHeader ? <div className='header'>{props.fullHeader}</div> : props.title ? <div className='header'><h4 className='title'>{props.title}</h4></div> : ''}
      <div className='content'>{props.children}</div>
      {props.btn ? <div className={`settingsEbtn`}><Button label={props.btnLabel} icon={props.btnIcon} onClick={props.btnOnClick}/></div> : ''}
    </div>
  )
}

export default SettingsElement